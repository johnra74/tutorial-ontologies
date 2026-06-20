package me.johnra.tutorial.finance.semantic.config;

import me.johnra.tutorial.finance.semantic.service.llm.AnthropicLlmClient;
import me.johnra.tutorial.finance.semantic.service.llm.LlmClient;
import me.johnra.tutorial.finance.semantic.service.llm.OllamaLlmClient;
import me.johnra.tutorial.finance.semantic.service.llm.OpenAiLlmClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

@Configuration
public class LlmConfig {

    @Value("${llm.model}")
    private String model;

    @Bean
    @ConditionalOnProperty(name = "llm.provider", havingValue = "ollama", matchIfMissing = true)
    public LlmClient ollamaLlmClient(
            @Value("${ollama.base-url:http://localhost:11434}") String baseUrl) {
        RestClient client = RestClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader("Accept",       MediaType.APPLICATION_JSON_VALUE)
            .build();
        return new OllamaLlmClient(client, model);
    }

    @Bean
    @ConditionalOnProperty(name = "llm.provider", havingValue = "openai")
    public LlmClient openAiLlmClient(
            @Value("${openai.base-url:https://api.openai.com}") String baseUrl,
            @Value("${openai.api-key:}") String apiKey) {
        RestClient client = RestClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader("Content-Type",  MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader("Accept",        MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader("Authorization", "Bearer " + apiKey)
            .build();
        return new OpenAiLlmClient(client, model);
    }

    @Bean
    @ConditionalOnProperty(name = "llm.provider", havingValue = "anthropic")
    public LlmClient anthropicLlmClient(
            @Value("${anthropic.api-key:}") String apiKey) {
        RestClient client = RestClient.builder()
            .baseUrl("https://api.anthropic.com")
            .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader("Accept",       MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader("x-api-key",    apiKey)
            .build();
        return new AnthropicLlmClient(client, model);
    }
}
