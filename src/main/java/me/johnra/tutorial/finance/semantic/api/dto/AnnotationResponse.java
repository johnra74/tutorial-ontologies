package me.johnra.tutorial.finance.semantic.api.dto;

import me.johnra.tutorial.finance.semantic.domain.AnnotationRound;
import me.johnra.tutorial.finance.semantic.domain.DatasetAnnotation;
import me.johnra.tutorial.finance.semantic.domain.OdrlTermsDocument;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Result of a multi-agent annotation session, including all rounds and final Turtle export")
public record AnnotationResponse(

    @Schema(description = "Unique session identifier (UUID)", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    String sessionId,

    @Schema(description = "Number of proposal-review rounds completed", example = "2")
    int totalRounds,

    @Schema(description = "Whether the ReviewerAgent approved the final annotation set", example = "true")
    boolean approved,

    @Schema(description = "Whether the final annotation passed Apache Jena SHACL validation", example = "true")
    boolean shaclConforms,

    @Schema(description = "Ordered list of all rounds, each containing proposals and reviewer feedback")
    List<AnnotationRound> rounds,

    @Schema(description = "Final merged dataset annotation (null if no rounds completed)")
    DatasetAnnotation finalAnnotation,

    @Schema(description = "Turtle-serialized RDF of the final annotation, including DCAT, DPV, ODRL and CSV-W triples")
    String turtle,

    @Schema(description = "Structured ODRL Terms & Conditions document produced by the TermsAgent after annotation converges")
    OdrlTermsDocument termsDocument,

    @Schema(description = "Turtle-serialized standalone ODRL Agreement from the TermsAgent (richer than the inline ODRL in the main Turtle export)")
    String termsTurtle,

    @Schema(description = """
        DPV personal-data category IRIs applicable to this dataset, including categories
        inferred via rdfs:subClassOf transitivity (e.g. dpv-pd:EmailAddress is inferred
        to be a dpv:PersonalData via dpv-pd:Identifying). Without OWL inference only direct
        subclasses of dpv:PersonalData would be visible to a single-hop SPARQL query.
        """)
    List<String> inferredPersonalDataCategories

) {}
