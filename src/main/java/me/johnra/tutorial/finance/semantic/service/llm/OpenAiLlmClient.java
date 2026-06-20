package me.johnra.tutorial.finance.semantic.service.llm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

public class OpenAiLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiLlmClient.class);

    private final RestClient restClient;
    private final String model;

    public OpenAiLlmClient(RestClient restClient, String model) {
        this.restClient = restClient;
        this.model = model;
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        log.info("LLM request  provider=openai model={} userPrompt={}chars",
            model, userPrompt.length());
        long start = System.currentTimeMillis();

        var body = Map.of(
            "model", model,
            "messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user",   "content", userPrompt)
            ),
            "response_format", Map.of("type", "json_object")
        );

        var response = restClient.post()
            .uri("/v1/chat/completions")
            .body(body)
            .retrieve()
            .body(OpenAiChatResponse.class);

        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new LlmException("Empty response from OpenAI");
        }
        String content = response.choices().get(0).message().content();
        log.info("LLM response provider=openai model={} responseChars={} elapsed={}ms",
            model, content.length(), System.currentTimeMillis() - start);
        return content;
    }

    record OpenAiChatResponse(List<Choice> choices) {}
    record Choice(OpenAiMessage message) {}
    record OpenAiMessage(String role, String content) {}
}
