package me.johnra.tutorial.finance.semantic.agent.review;

import me.johnra.tutorial.finance.semantic.agent.Agent;
import me.johnra.tutorial.finance.semantic.domain.DatasetAnnotation;
import me.johnra.tutorial.finance.semantic.domain.FeedbackItem;
import me.johnra.tutorial.finance.semantic.domain.OdrlAgreementProposal;
import me.johnra.tutorial.finance.semantic.domain.ReviewFeedback;
import me.johnra.tutorial.finance.semantic.service.llm.LlmClient;
import me.johnra.tutorial.finance.semantic.service.llm.LlmException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ReviewerAgent implements Agent<DatasetAnnotation, ReviewFeedback> {

    private static final Logger log = LoggerFactory.getLogger(ReviewerAgent.class);

    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;

    public ReviewerAgent(LlmClient llmClient, ObjectMapper objectMapper) {
        this.llmClient = llmClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public ReviewFeedback process(DatasetAnnotation input) {
        log.info("REVIEW  dataset='{}' columns={}", input.descriptor().title(),
            input.columnAnnotations().size());

        String userPrompt = ReviewerPrompts.buildUserPrompt(input, objectMapper);
        String raw = llmClient.complete(ReviewerPrompts.SYSTEM, userPrompt);

        ReviewFeedback result = parseResponse(raw);

        long errors   = result.items().stream().filter(i -> i.severity() == FeedbackItem.Severity.ERROR).count();
        long warnings = result.items().stream().filter(i -> i.severity() == FeedbackItem.Severity.WARNING).count();
        long infos    = result.items().stream().filter(i -> i.severity() == FeedbackItem.Severity.INFO).count();

        log.info("REVIEW  decision={} errors={} warnings={} infos={} — {}",
            errors == 0 ? "APPROVE" : "REJECT",
            errors, warnings, infos,
            result.overallNote());

        result.items().forEach(item -> {
            String level = switch (item.severity()) {
                case ERROR   -> "REVIEW  ✗ ERROR  ";
                case WARNING -> "REVIEW  ⚠ WARN   ";
                case INFO    -> "REVIEW  ℹ INFO   ";
            };
            String suggestion = item.suggestion() != null ? " → " + item.suggestion() : "";
            log.info("{} [{}] {}{}", level, item.columnName(), item.message(), suggestion);
        });

        if (result.odrlAgreement() != null) {
            log.info("REVIEW  ODRL assigner={} assignee={} termEnd={}",
                result.odrlAgreement().assignerIri(),
                result.odrlAgreement().assigneeIri(),
                result.odrlAgreement().termEndDateTime());
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private ReviewFeedback parseResponse(String raw) {
        try {
            var json = objectMapper.readValue(extractJson(raw), Map.class);

            boolean approved = Boolean.TRUE.equals(json.get("approved"));
            String overallNote = (String) json.get("overallNote");

            List<FeedbackItem> items = parseFeedbackItems(
                (List<Map<String, Object>>) json.getOrDefault("items", List.of()));

            OdrlAgreementProposal odrl = parseOdrlAgreement(
                (Map<String, Object>) json.get("odrlAgreement"));

            return new ReviewFeedback(approved, overallNote, items, odrl);
        } catch (Exception e) {
            throw new LlmException("Failed to parse ReviewerAgent response: " + raw, e);
        }
    }

    private List<FeedbackItem> parseFeedbackItems(List<Map<String, Object>> rawItems) {
        return rawItems.stream().map(item -> {
            String col      = (String) item.getOrDefault("columnName", "dataset");
            String sevRaw   = (String) item.getOrDefault("severity", "INFO");
            String message  = (String) item.getOrDefault("message", "(no message)");
            String suggest  = (String) item.get("suggestion");
            FeedbackItem.Severity severity;
            try {
                severity = FeedbackItem.Severity.valueOf(sevRaw.toUpperCase());
            } catch (IllegalArgumentException e) {
                severity = FeedbackItem.Severity.INFO;
            }
            return new FeedbackItem(col, severity, message, suggest);
        }).toList();
    }

    @SuppressWarnings("unchecked")
    private OdrlAgreementProposal parseOdrlAgreement(Map<String, Object> raw) {
        if (raw == null) return null;
        return new OdrlAgreementProposal(
            (String) raw.get("assignerIri"),
            (String) raw.get("assigneeIri"),
            (String) raw.get("termEndDateTime"),
            (String) raw.get("spatialConstraintIri"),
            (List<String>) raw.getOrDefault("permittedPurposeIris",    List.of()),
            (List<String>) raw.getOrDefault("prohibitedActionIris",     List.of()),
            (List<String>) raw.getOrDefault("obligationActionIris",     List.of())
        );
    }

    private String extractJson(String raw) {
        String trimmed = raw.strip();
        if (trimmed.startsWith("```")) {
            int start = trimmed.indexOf('\n') + 1;
            int end = trimmed.lastIndexOf("```");
            if (end > start) {
                return trimmed.substring(start, end).strip();
            }
        }
        return trimmed;
    }
}
