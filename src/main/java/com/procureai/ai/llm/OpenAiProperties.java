package com.procureai.ai.llm;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "procureai.ai.openai")
public record OpenAiProperties(
        String apiKey,
        String model,
        String baseUrl,
        int timeoutSeconds
) {
}
