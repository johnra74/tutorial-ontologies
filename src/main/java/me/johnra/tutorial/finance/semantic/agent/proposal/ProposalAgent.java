package me.johnra.tutorial.finance.semantic.agent.proposal;

import me.johnra.tutorial.finance.semantic.agent.Agent;
import me.johnra.tutorial.finance.semantic.agent.ProposalInput;
import me.johnra.tutorial.finance.semantic.domain.ColumnAnnotation;
import me.johnra.tutorial.finance.semantic.service.llm.LlmClient;
import me.johnra.tutorial.finance.semantic.service.llm.LlmException;
import me.johnra.tutorial.finance.semantic.vocabulary.PersonalDataCategory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ProposalAgent implements Agent<ProposalInput, ColumnAnnotation> {

    private static final Logger log = LoggerFactory.getLogger(ProposalAgent.class);

    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;

    public ProposalAgent(LlmClient llmClient, ObjectMapper objectMapper) {
        this.llmClient = llmClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public ColumnAnnotation process(ProposalInput input) {
        boolean isRevision = !input.priorFeedback().isEmpty();
        if (isRevision) {
            log.info("PROPOSE [{}] revising — {} prior feedback item(s): {}",
                input.column().name(),
                input.priorFeedback().size(),
                input.priorFeedback().stream()
                    .map(f -> "[" + f.severity() + "] " + f.message())
                    .toList());
        } else {
            log.info("PROPOSE [{}] first pass (datatype={})",
                input.column().name(), input.column().datatype());
        }

        String userPrompt = ProposalPrompts.buildUserPrompt(input.column(), input.priorFeedback());
        String raw = llmClient.complete(ProposalPrompts.SYSTEM, userPrompt);

        ColumnAnnotation result = parseResponse(raw, input);

        log.info("PROPOSE [{}] → propertyUrl={} vocabulary={} privacy={}",
            result.column().name(),
            result.propertyUrl(),
            result.vocabulary(),
            result.personalDataCategory());

        return result;
    }

    @SuppressWarnings("unchecked")
    private ColumnAnnotation parseResponse(String raw, ProposalInput input) {
        try {
            var json = objectMapper.readValue(extractJson(raw), Map.class);

            String propertyUrl = (String) json.get("propertyUrl");
            String vocabulary  = (String) json.get("vocabulary");
            String pdcRaw      = (String) json.get("personalDataCategory");
            String note        = (String) json.get("semanticNote");

            PersonalDataCategory pdc = parsePersonalDataCategory(pdcRaw);

            return new ColumnAnnotation(input.column(), propertyUrl, vocabulary, pdc, note);
        } catch (Exception e) {
            throw new LlmException("Failed to parse ProposalAgent response for column '"
                + input.column().name() + "': " + raw, e);
        }
    }

    private PersonalDataCategory parsePersonalDataCategory(String raw) {
        if (raw == null) return PersonalDataCategory.NONE;
        try {
            return PersonalDataCategory.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown personalDataCategory '{}', defaulting to NONE", raw);
            return PersonalDataCategory.NONE;
        }
    }

    /** Strip markdown code fences if the LLM wrapped its JSON in them. */
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
