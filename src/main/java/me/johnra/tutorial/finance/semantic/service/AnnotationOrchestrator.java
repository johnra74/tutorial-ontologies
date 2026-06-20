package me.johnra.tutorial.finance.semantic.service;

import me.johnra.tutorial.finance.semantic.agent.ProposalInput;
import me.johnra.tutorial.finance.semantic.agent.proposal.ProposalAgent;
import me.johnra.tutorial.finance.semantic.agent.review.ReviewerAgent;
import me.johnra.tutorial.finance.semantic.agent.terms.TermsAgent;
import me.johnra.tutorial.finance.semantic.domain.*;
import me.johnra.tutorial.finance.semantic.vocabulary.LegalBasis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
public class AnnotationOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(AnnotationOrchestrator.class);

    private final ProposalAgent proposalAgent;
    private final ReviewerAgent reviewerAgent;
    private final TermsAgent termsAgent;
    private final ShaclValidationService shaclValidationService;
    private final RdfExportService rdfExportService;
    private final OwlInferenceService owlInferenceService;
    private final int maxRounds;
    private final ExecutorService executor;

    public AnnotationOrchestrator(
            ProposalAgent proposalAgent,
            ReviewerAgent reviewerAgent,
            TermsAgent termsAgent,
            ShaclValidationService shaclValidationService,
            RdfExportService rdfExportService,
            OwlInferenceService owlInferenceService,
            @Value("${annotation.max-rounds:5}") int maxRounds) {
        this.proposalAgent = proposalAgent;
        this.reviewerAgent = reviewerAgent;
        this.termsAgent = termsAgent;
        this.shaclValidationService = shaclValidationService;
        this.rdfExportService = rdfExportService;
        this.owlInferenceService = owlInferenceService;
        this.maxRounds = maxRounds;
        // Virtual threads — one per column proposal, no blocking the carrier thread
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
    }

    public AnnotationSession annotate(DatasetDescriptor descriptor) {
        String sessionId = UUID.randomUUID().toString();
        AnnotationSession session = new AnnotationSession(sessionId);

        MDC.put("sessionId", sessionId);
        long sessionStart = System.currentTimeMillis();
        log.info("Starting annotation session for dataset '{}' ({} columns, max {} rounds)",
            descriptor.title(), descriptor.columns().size(), maxRounds);

        Map<String, List<FeedbackItem>> feedbackByColumn = new HashMap<>();
        DatasetAnnotation currentAnnotation = null;
        org.apache.jena.rdf.model.Model lastRdfModel = null;

        try {
            for (int round = 1; round <= maxRounds; round++) {
                MDC.put("round", String.valueOf(round));
                long roundStart = System.currentTimeMillis();
                log.info("--- Round {}/{} started ---", round, maxRounds);

                List<ColumnAnnotation> proposals = proposeAll(descriptor, feedbackByColumn);
                logProposalSummary(proposals);

                currentAnnotation = buildDatasetAnnotation(descriptor, proposals);

                ReviewFeedback feedback = reviewerAgent.process(currentAnnotation);

                lastRdfModel = rdfExportService.buildModel(currentAnnotation, feedback.odrlAgreement());
                var shaclResult = shaclValidationService.validate(lastRdfModel);
                feedback = mergeShaclViolations(feedback, shaclResult);

                session.addRound(new AnnotationRound(round, proposals, feedback));

                log.info("--- Round {}/{} completed in {}ms | approved={} shaclConforms={} ---",
                    round, maxRounds, System.currentTimeMillis() - roundStart,
                    feedback.approved(), shaclResult.conforms());

                // Approval is deterministic: no ERRORs + SHACL passes.
                // The LLM's approved boolean is advisory; hasErrors() is the ground truth.
                boolean canApprove = !feedback.hasErrors() && shaclResult.conforms();
                if (canApprove) {
                    log.info("Session approved after {} round(s) in {}ms",
                        round, System.currentTimeMillis() - sessionStart);
                    session.complete(currentAnnotation, true);
                    enrichWithInference(session, lastRdfModel);
                    generateTerms(session, currentAnnotation);
                    return session;
                }

                // Only route ERROR items to the next proposal round; warnings are informational
                feedbackByColumn = groupFeedbackByColumn(feedback.items().stream()
                    .filter(i -> i.severity() == FeedbackItem.Severity.ERROR)
                    .toList());
            }

            log.warn("Session reached max rounds ({}) without full approval ({}ms elapsed)",
                maxRounds, System.currentTimeMillis() - sessionStart);
            session.complete(currentAnnotation, false);
            enrichWithInference(session, lastRdfModel);
            generateTerms(session, currentAnnotation);
            return session;

        } finally {
            MDC.remove("round");
            MDC.remove("sessionId");
        }
    }

    private void enrichWithInference(AnnotationSession session,
                                      org.apache.jena.rdf.model.Model rdfModel) {
        if (rdfModel == null) return;
        log.info("--- OWL inference started ---");
        long start = System.currentTimeMillis();
        try {
            List<String> categories = owlInferenceService.inferredPersonalDataCategories(rdfModel);
            session.setInferredPersonalDataCategories(categories);
            log.info("--- OWL inference completed in {}ms ({} categories) ---",
                System.currentTimeMillis() - start, categories.size());
        } catch (Exception e) {
            log.error("OWL inference failed (non-fatal): {}", e.getMessage());
        }
    }

    private void generateTerms(AnnotationSession session, DatasetAnnotation annotation) {
        if (annotation == null) return;
        log.info("--- Terms generation started ---");
        long start = System.currentTimeMillis();
        try {
            OdrlTermsDocument terms = termsAgent.process(annotation);
            session.setTermsDocument(terms);
            log.info("--- Terms generation completed in {}ms ---",
                System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.error("Terms generation failed (non-fatal): {}", e.getMessage());
        }
    }

    private void logProposalSummary(List<ColumnAnnotation> proposals) {
        log.info("PROPOSE summary ({} columns):", proposals.size());
        proposals.forEach(p -> log.info("  {}",
            String.format("%-18s %-12s %-60s privacy=%s",
                p.column().name(),
                p.vocabulary()  != null ? p.vocabulary()  : "?",
                p.propertyUrl() != null ? p.propertyUrl() : "?",
                p.personalDataCategory())));
    }

    private List<ColumnAnnotation> proposeAll(
            DatasetDescriptor descriptor,
            Map<String, List<FeedbackItem>> feedbackByColumn) {

        // Snapshot MDC so virtual threads (which don't inherit thread-locals) can restore it
        Map<String, String> mdcSnapshot = MDC.getCopyOfContextMap();

        List<CompletableFuture<ColumnAnnotation>> futures = descriptor.columns().stream()
            .map(col -> {
                List<FeedbackItem> fb = feedbackByColumn.getOrDefault(col.name(), List.of());
                ProposalInput input = new ProposalInput(col, fb);
                return CompletableFuture.supplyAsync(() -> {
                    if (mdcSnapshot != null) MDC.setContextMap(mdcSnapshot);
                    try {
                        return proposalAgent.process(input);
                    } finally {
                        MDC.clear();
                    }
                }, executor);
            })
            .toList();

        return futures.stream()
            .map(CompletableFuture::join)
            .toList();
    }

    private DatasetAnnotation buildDatasetAnnotation(
            DatasetDescriptor descriptor,
            List<ColumnAnnotation> proposals) {

        DatasetLevelDpv dpv = new DatasetLevelDpv(
            descriptor.publisherIri(),
            List.of(LegalBasis.CONTRACT, LegalBasis.LEGAL_OBLIGATION),
            descriptor.purposeIris(),
            List.of("https://w3id.org/dpv#Use", "https://w3id.org/dpv#Store", "https://w3id.org/dpv#Share"),
            List.of("https://example.org/authorised-counterparty"),
            List.of("https://w3id.org/dpv#AccessControlMethod", "https://w3id.org/dpv#Encryption")
        );

        return new DatasetAnnotation(descriptor, proposals, dpv);
    }

    private ReviewFeedback mergeShaclViolations(ReviewFeedback feedback,
                                                 ShaclValidationService.ValidationResult shaclResult) {
        if (shaclResult.conforms()) return feedback;

        List<FeedbackItem> merged = new ArrayList<>(feedback.items());
        shaclResult.violations().forEach(v ->
            merged.add(new FeedbackItem("dataset", FeedbackItem.Severity.ERROR,
                "SHACL violation: " + v, "Fix the RDF structure to satisfy the constraint")));

        // Re-evaluate approved flag: errors from SHACL force rejection
        return new ReviewFeedback(false, feedback.overallNote(), merged, feedback.odrlAgreement());
    }

    private Map<String, List<FeedbackItem>> groupFeedbackByColumn(List<FeedbackItem> items) {
        return items.stream()
            .filter(i -> i.columnName() != null)
            .collect(Collectors.groupingBy(FeedbackItem::columnName));
    }
}
