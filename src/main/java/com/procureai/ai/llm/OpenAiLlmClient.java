package com.procureai.ai.llm;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@ConditionalOnExpression("'${procureai.ai.openai.api-key:}' != ''")
public class OpenAiLlmClient implements LlmClient {

    private final OpenAiProperties properties;
    private final RestClient restClient;

    public OpenAiLlmClient(OpenAiProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
        requestFactory.setReadTimeout(Duration.ofSeconds(properties.timeoutSeconds()));
        this.restClient = restClientBuilder
                .requestFactory(requestFactory)
                .baseUrl(properties.baseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        Map<String, Object> request = Map.of(
                "model", properties.model(),
                "input", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                )
        );

        try {
            JsonNode response = restClient.post()
                    .uri("/responses")
                    .body(request)
                    .retrieve()
                    .body(JsonNode.class);
            return extractText(response);
        } catch (RestClientException exception) {
            throw new IllegalStateException("OpenAI request failed: " + exception.getMessage(), exception);
        }
    }

    private String extractText(JsonNode response) {
        if (response == null) {
            throw new IllegalStateException("OpenAI returned an empty response");
        }

        String outputText = response.path("output_text").asText();
        if (!outputText.isBlank()) {
            return outputText;
        }

        JsonNode output = response.path("output");
        if (output.isArray()) {
            for (JsonNode item : output) {
                JsonNode content = item.path("content");
                if (!content.isArray()) {
                    continue;
                }
                for (JsonNode contentItem : content) {
                    String text = contentItem.path("text").asText();
                    if (!text.isBlank()) {
                        return text;
                    }
                }
            }
        }

        throw new IllegalStateException("OpenAI response did not contain generated text");
    }
}
