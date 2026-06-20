package me.johnra.tutorial.finance.semantic.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Result of running SHACL validation against a session's final annotation")
public record ValidationResponse(

    @Schema(description = "Session identifier", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    String sessionId,

    @Schema(description = "True if the annotation graph satisfies all SHACL shapes", example = "true")
    boolean conforms,

    @Schema(description = "Human-readable violation messages (empty when conforms=true)")
    List<String> violations

) {}
