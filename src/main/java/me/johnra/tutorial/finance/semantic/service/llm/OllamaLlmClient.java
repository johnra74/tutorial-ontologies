package me.johnra.tutorial.finance.semantic.service.llm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

public class OllamaLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(OllamaLlmClient.class);

    private final RestClient restClient;
    private final String model;

    public OllamaLlmClient(RestClient restClient, String model) {
        this.restClient = restClient;
        this.model = model;
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        log.info("LLM request  provider=ollama model={} userPrompt={}chars",
            model, userPrompt.length());
        long start = System.currentTimeMillis();

        var body = Map.of(
            "model", model,
            "messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user",   "content", userPrompt)
            ),
            "stream", false,
            "format", "json"
        );

        var response = restClient.post()
            .uri("/api/chat")
            .body(body)
            .retrieve()
            .body(OllamaChatResponse.class);

        if (response == null || response.message() == null) {
            throw new LlmException("Empty response from Ollama");
        }
        String content = response.message().content();
        log.info("LLM response provider=ollama model={} responseChars={} elapsed={}ms",
            model, content.length(), System.currentTimeMillis() - start);
        return content;
    }

    record OllamaChatResponse(OllamaMessage message) {}
    record OllamaMessage(String role, String content) {}
}
