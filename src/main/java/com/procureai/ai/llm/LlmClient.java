package com.procureai.ai.llm;

public interface LlmClient {

    String complete(String systemPrompt, String userPrompt);
}
