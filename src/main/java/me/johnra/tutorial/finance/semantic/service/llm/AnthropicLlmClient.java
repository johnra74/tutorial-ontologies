package me.johnra.tutorial.finance.semantic.service.llm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

public class AnthropicLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(AnthropicLlmClient.class);

    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final RestClient restClient;
    private final String model;

    public AnthropicLlmClient(RestClient restClient, String model) {
        this.restClient = restClient;
        this.model = model;
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        log.info("LLM request  provider=anthropic model={} userPrompt={}chars",
            model, userPrompt.length());
        long start = System.currentTimeMillis();

        var body = Map.of(
            "model", model,
            "max_tokens", 4096,
            "system", systemPrompt,
            "messages", List.of(
                Map.of("role", "user", "content", userPrompt)
            )
        );

        var response = restClient.post()
            .uri("/v1/messages")
            .header("anthropic-version", ANTHROPIC_VERSION)
            .body(body)
            .retrieve()
            .body(AnthropicResponse.class);

        if (response == null || response.content() == null || response.content().isEmpty()) {
            throw new LlmException("Empty response from Anthropic");
        }
        String content = response.content().get(0).text();
        log.info("LLM response provider=anthropic model={} responseChars={} elapsed={}ms",
            model, content.length(), System.currentTimeMillis() - start);
        return content;
    }

    record AnthropicResponse(List<ContentBlock> content) {}
    record ContentBlock(String type, String text) {}
}
