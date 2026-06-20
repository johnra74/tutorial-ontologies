package me.johnra.tutorial.finance.semantic.api;

import me.johnra.tutorial.finance.semantic.api.dto.AnnotationRequest;
import me.johnra.tutorial.finance.semantic.api.dto.AnnotationResponse;
import me.johnra.tutorial.finance.semantic.api.dto.ValidationResponse;
import me.johnra.tutorial.finance.semantic.domain.AnnotationSession;
import me.johnra.tutorial.finance.semantic.domain.DatasetDescriptor;
import me.johnra.tutorial.finance.semantic.domain.OdrlAgreementProposal;
import me.johnra.tutorial.finance.semantic.service.AnnotationOrchestrator;
import me.johnra.tutorial.finance.semantic.service.RdfExportService;
import me.johnra.tutorial.finance.semantic.service.ShaclValidationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/v1/annotations")
@Tag(name = "Annotations", description = "Multi-agent semantic annotation of finance datasets")
public class AnnotationController {

    private final AnnotationOrchestrator orchestrator;
    private final RdfExportService rdfExportService;
    private final ShaclValidationService shaclValidationService;

    // In-memory session store — replace with a persistent store for production
    private final Map<String, AnnotationSession> sessions = new ConcurrentHashMap<>();

    public AnnotationController(AnnotationOrchestrator orchestrator,
                                 RdfExportService rdfExportService,
                                 ShaclValidationService shaclValidationService) {
        this.orchestrator = orchestrator;
        this.rdfExportService = rdfExportService;
        this.shaclValidationService = shaclValidationService;
    }

    @Operation(
        summary = "Annotate a dataset",
        description = """
            Runs the multi-agent annotation loop against the supplied dataset descriptor.
            The ProposalAgent maps each column to a schema.org/FIBO propertyUrl and DPV-PD
            privacy category; the ReviewerAgent checks the full set for consistency, SHACL
            compliance, and proposes the ODRL Agreement. Iteration continues until both agents
            agree or the configured `annotation.max-rounds` is reached.
            The response includes all rounds, the final annotation, and a Turtle RDF export.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Annotation session completed",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                               schema = @Schema(implementation = AnnotationResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request body",
            content = @Content)
    })
    @PostMapping
    public ResponseEntity<AnnotationResponse> annotate(@Valid @RequestBody AnnotationRequest request) {
        DatasetDescriptor descriptor = new DatasetDescriptor(
            request.datasetTitle(),
            request.publisherIri(),
            request.purposeIris(),
            request.columns()
        );

        AnnotationSession session = orchestrator.annotate(descriptor);
        sessions.put(session.sessionId(), session);

        return ResponseEntity.ok(toResponse(session));
    }

    @Operation(
        summary = "Retrieve a session",
        description = "Returns the full annotation session by ID, including all rounds and Turtle export."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Session found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                               schema = @Schema(implementation = AnnotationResponse.class))),
        @ApiResponse(responseCode = "404", description = "Session not found",
            content = @Content)
    })
    @GetMapping("/{sessionId}")
    public ResponseEntity<AnnotationResponse> getSession(
            @Parameter(description = "UUID returned by POST /api/v1/annotations", required = true)
            @PathVariable String sessionId) {
        AnnotationSession session = sessions.get(sessionId);
        if (session == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toResponse(session));
    }

    @Operation(
        summary = "Re-validate a session",
        description = """
            Rebuilds the RDF graph from the session's final annotation and runs all four
            SHACL shapes from the finance-restricted profile. Useful to confirm that a
            session which completed before SHACL was fully wired still satisfies the shapes.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Validation result",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                               schema = @Schema(implementation = ValidationResponse.class))),
        @ApiResponse(responseCode = "400", description = "Session has no final annotation yet",
            content = @Content),
        @ApiResponse(responseCode = "404", description = "Session not found",
            content = @Content)
    })
    @PostMapping("/{sessionId}/validate")
    public ResponseEntity<ValidationResponse> validate(
            @Parameter(description = "UUID of the session to validate", required = true)
            @PathVariable String sessionId) {
        AnnotationSession session = sessions.get(sessionId);
        if (session == null) {
            return ResponseEntity.notFound().build();
        }
        if (session.finalAnnotation() == null) {
            return ResponseEntity.badRequest().build();
        }

        OdrlAgreementProposal odrl = session.rounds().stream()
            .filter(r -> r.feedback() != null && r.feedback().odrlAgreement() != null)
            .reduce((first, second) -> second)
            .map(r -> r.feedback().odrlAgreement())
            .orElse(null);

        var model = rdfExportService.buildModel(session.finalAnnotation(), odrl);
        var result = shaclValidationService.validate(model);

        return ResponseEntity.ok(new ValidationResponse(sessionId, result.conforms(), result.violations()));
    }

    private AnnotationResponse toResponse(AnnotationSession session) {
        boolean approved = session.approved();

        OdrlAgreementProposal odrl = session.rounds().stream()
            .filter(r -> r.feedback() != null && r.feedback().odrlAgreement() != null)
            .reduce((first, second) -> second)
            .map(r -> r.feedback().odrlAgreement())
            .orElse(null);

        String turtle = session.finalAnnotation() != null
            ? rdfExportService.toTurtle(rdfExportService.buildModel(session.finalAnnotation(), odrl))
            : null;

        String termsTurtle = session.termsDocument() != null
            ? rdfExportService.toTurtle(rdfExportService.buildTermsModel(session.termsDocument()))
            : null;

        return new AnnotationResponse(
            session.sessionId(),
            session.rounds().size(),
            approved,
            session.shaclConforms(),
            session.rounds(),
            session.finalAnnotation(),
            turtle,
            session.termsDocument(),
            termsTurtle,
            session.inferredPersonalDataCategories()
        );
    }
}
