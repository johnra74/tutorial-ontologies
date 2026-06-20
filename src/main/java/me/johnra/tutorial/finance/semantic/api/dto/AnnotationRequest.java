package me.johnra.tutorial.finance.semantic.api.dto;

import me.johnra.tutorial.finance.semantic.domain.DataColumn;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

@Schema(description = "Request to annotate a finance dataset's columns with semantic metadata")
public record AnnotationRequest(

    @Schema(description = "Human-readable dataset title", example = "Customer accounts master")
    @NotBlank String datasetTitle,

    @Schema(description = "IRI of the data publisher / controller",
            example = "https://example.org/org-acme-bank")
    @NotBlank String publisherIri,

    @Schema(description = "List of DPV purpose IRIs that govern processing",
            example = "[\"https://w3id.org/dpv#CreditChecking\",\"https://w3id.org/dpv#FraudPreventionAndDetection\"]")
    @NotEmpty List<String> purposeIris,

    @Schema(description = "CSV columns to annotate — name and XSD datatype")
    @NotEmpty List<DataColumn> columns

) {}
