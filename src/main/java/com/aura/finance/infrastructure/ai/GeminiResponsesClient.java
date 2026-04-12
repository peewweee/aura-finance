package com.aura.finance.infrastructure.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class GeminiResponsesClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String modelName;
    private final String apiKey;

    public GeminiResponsesClient(
            String baseUrl,
            String apiKey,
            String modelName,
            ObjectMapper objectMapper
    ) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("A hosted AI API key is required. Set AURA_AI_GEMINI_API_KEY before starting the app.");
        }

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.objectMapper = objectMapper;
        this.modelName = modelName;
        this.apiKey = apiKey;
    }

    public String generateText(String prompt, double temperature) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("contents", java.util.List.of(
                Map.of(
                        "parts", java.util.List.of(
                                Map.of("text", prompt)
                        )
                )
        ));
        payload.put("generationConfig", Map.of("temperature", temperature));

        try {
            JsonNode root = restClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1beta/models/{model}:generateContent")
                            .queryParam("key", apiKey)
                            .build(modelName)
                    )
                    .header("x-goog-api-key", apiKey)
                    .body(payload)
                    .retrieve()
                    .body(JsonNode.class);

            if (root == null) {
                throw new AiIntegrationException("Hosted AI API returned an empty response.");
            }

            String outputText = extractOutputText(root).trim();
            if (outputText.isBlank()) {
                throw new AiIntegrationException("Hosted AI API returned a response without any text output.");
            }

            return outputText;
        } catch (RestClientResponseException exception) {
            String responseBody = exception.getResponseBodyAsString();
            throw new AiIntegrationException(
                    "Hosted AI API request failed with status %d: %s"
                            .formatted(exception.getStatusCode().value(), responseBody),
                    exception
            );
        } catch (RuntimeException exception) {
            if (exception instanceof AiIntegrationException aiIntegrationException) {
                throw aiIntegrationException;
            }
            throw new AiIntegrationException("Failed to call the hosted AI API.", exception);
        }
    }

    private String extractOutputText(JsonNode root) {
        JsonNode candidates = root.path("candidates");
        if (candidates.isArray()) {
            for (JsonNode candidate : candidates) {
                JsonNode parts = candidate.path("content").path("parts");
                if (!parts.isArray()) {
                    continue;
                }

                StringBuilder text = new StringBuilder();
                for (JsonNode part : parts) {
                    if (part.hasNonNull("text") && part.get("text").isTextual()) {
                        if (!text.isEmpty()) {
                            text.append('\n');
                        }
                        text.append(part.get("text").asText());
                    }
                }

                if (!text.isEmpty()) {
                    return text.toString();
                }
            }
        }

        throw new AiIntegrationException("Hosted AI API response did not include usable text output: " + compactJson(root));
    }

    private String compactJson(JsonNode root) {
        try {
            return objectMapper.writeValueAsString(root);
        } catch (IOException exception) {
            return root.toString();
        }
    }
}
