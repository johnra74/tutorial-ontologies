package me.johnra.tutorial.finance.semantic.agent.terms;

import me.johnra.tutorial.finance.semantic.agent.Agent;
import me.johnra.tutorial.finance.semantic.domain.*;
import me.johnra.tutorial.finance.semantic.service.llm.LlmClient;
import me.johnra.tutorial.finance.semantic.service.llm.LlmException;
import me.johnra.tutorial.finance.semantic.vocabulary.PersonalDataCategory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class TermsAgent implements Agent<DatasetAnnotation, OdrlTermsDocument> {

    private static final Logger log = LoggerFactory.getLogger(TermsAgent.class);

    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;

    public TermsAgent(LlmClient llmClient, ObjectMapper objectMapper) {
        this.llmClient = llmClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public OdrlTermsDocument process(DatasetAnnotation input) {
        List<String> pdCategories = input.columnAnnotations().stream()
            .map(ColumnAnnotation::personalDataCategory)
            .filter(PersonalDataCategory::isPersonalData)
            .distinct()
            .map(PersonalDataCategory::name)
            .toList();

        log.info("TERMS   dataset='{}' columns={} personalDataCategories={}",
            input.descriptor().title(),
            input.columnAnnotations().size(),
            pdCategories);

        String userPrompt = TermsPrompts.buildUserPrompt(input, objectMapper);
        String raw = llmClient.complete(TermsPrompts.SYSTEM, userPrompt);

        OdrlTermsDocument result = parseResponse(raw, input);

        log.info("TERMS   policyUid={} permissions={} prohibitions={} obligations={} retentionPeriod={}",
            result.policyUid(),
            result.permissions()  != null ? result.permissions().size()  : 0,
            result.prohibitions() != null ? result.prohibitions().size() : 0,
            result.obligations()  != null ? result.obligations().size()  : 0,
            result.retentionPeriod());

        if (result.permissions() != null) {
            result.permissions().forEach(p ->
                log.info("TERMS   ✓ PERMIT  action={} purposes={}",
                    p.action(), p.purposeConstraintIris()));
        }
        if (result.prohibitions() != null) {
            result.prohibitions().forEach(p ->
                log.info("TERMS   ✗ PROHIBIT action={} — {}", p.action(), p.rationale()));
        }
        if (result.obligations() != null) {
            result.obligations().forEach(o ->
                log.info("TERMS   ⚑ OBLIGE  action={} trigger={}", o.action(), o.trigger()));
        }
        if (result.legalBasisIris() != null && !result.legalBasisIris().isEmpty()) {
            log.info("TERMS   legalBases={}", result.legalBasisIris());
        }
        if (result.overallNote() != null) {
            log.info("TERMS   note: {}", result.overallNote());
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private OdrlTermsDocument parseResponse(String raw, DatasetAnnotation input) {
        try {
            var json = objectMapper.readValue(extractJson(raw), Map.class);

            List<String> pdCategoryIris = input.columnAnnotations().stream()
                .map(ColumnAnnotation::personalDataCategory)
                .filter(PersonalDataCategory::isPersonalData)
                .distinct()
                .map(PersonalDataCategory::iri)
                .collect(Collectors.toList());

            return new OdrlTermsDocument(
                asString(json.getOrDefault("policyUid", "https://example.org/terms/agreement")),
                asString(json.get("assignerIri")),
                asString(json.getOrDefault("assigneeIri", "https://example.org/authorised-counterparty")),
                asString(json.get("issuedDate")),
                asString(json.get("termEndDateTime")),
                asString(json.getOrDefault("spatialConstraintIri", "https://example.org/region/EEA")),
                asString(json.get("governingLaw")),
                asString(json.getOrDefault("retentionPeriod", "P5Y")),
                parsePermissions((List<Map<String, Object>>) json.getOrDefault("permissions", List.of())),
                parseProhibitions((List<Map<String, Object>>) json.getOrDefault("prohibitions", List.of())),
                parseObligations((List<Map<String, Object>>) json.getOrDefault("obligations", List.of())),
                asStringList(json.getOrDefault("legalBasisIris", List.of())),
                pdCategoryIris,
                asString(json.get("overallNote"))
            );
        } catch (Exception e) {
            throw new LlmException("Failed to parse TermsAgent response: " + raw, e);
        }
    }

    /** Accepts String or List — joins list items with "; " to produce a single String. */
    @SuppressWarnings("unchecked")
    private String asString(Object value) {
        if (value == null) return null;
        if (value instanceof String s) return s;
        if (value instanceof List<?> list) return String.join("; ", (List<String>) list);
        return value.toString();
    }

    /** Accepts List<String> or a single String — always returns a List<String>. */
    @SuppressWarnings("unchecked")
    private List<String> asStringList(Object value) {
        if (value == null) return List.of();
        if (value instanceof List<?>) return (List<String>) value;
        if (value instanceof String s) return List.of(s);
        return List.of(value.toString());
    }

    @SuppressWarnings("unchecked")
    private List<OdrlPermission> parsePermissions(List<Map<String, Object>> raw) {
        return raw.stream().map(m -> new OdrlPermission(
            (String) m.get("action"),
            (List<String>) m.getOrDefault("purposeConstraintIris", List.of()),
            (List<String>) m.getOrDefault("recipientConstraintIris", List.of()),
            (String) m.get("temporalConstraint"),
            (String) m.get("duty")
        )).toList();
    }

    private List<OdrlProhibition> parseProhibitions(List<Map<String, Object>> raw) {
        return raw.stream().map(m -> new OdrlProhibition(
            (String) m.get("action"),
            (String) m.getOrDefault("rationale", "")
        )).toList();
    }

    private List<OdrlObligation> parseObligations(List<Map<String, Object>> raw) {
        return raw.stream().map(m -> new OdrlObligation(
            (String) m.get("action"),
            (String) m.get("trigger"),
            (String) m.getOrDefault("description", "")
        )).toList();
    }

    private String extractJson(String raw) {
        String trimmed = raw.strip();
        if (trimmed.startsWith("```")) {
            int start = trimmed.indexOf('\n') + 1;
            int end = trimmed.lastIndexOf("```");
            if (end > start) return trimmed.substring(start, end).strip();
        }
        return trimmed;
    }
}
