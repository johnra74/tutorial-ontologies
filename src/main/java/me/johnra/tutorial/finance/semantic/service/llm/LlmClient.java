package me.johnra.tutorial.finance.semantic.service.llm;

public interface LlmClient {
    /**
     * Send a system + user message pair and return the assistant's response text.
     */
    String complete(String systemPrompt, String userPrompt);
}
