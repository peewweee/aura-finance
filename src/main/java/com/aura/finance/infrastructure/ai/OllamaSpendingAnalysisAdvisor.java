package com.aura.finance.infrastructure.ai;

import com.aura.finance.application.port.out.SpendingAnalysisAdvisor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.ollama.OllamaChatModel;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OllamaSpendingAnalysisAdvisor implements SpendingAnalysisAdvisor {

    private final OllamaChatModel chatModel;
    private final ObjectMapper objectMapper;

    public OllamaSpendingAnalysisAdvisor(String baseUrl, String modelName, ObjectMapper objectMapper) {
        this.chatModel = OllamaChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(modelName)
                .temperature(0.2)
                .timeout(Duration.ofSeconds(60))
                .build();
        this.objectMapper = objectMapper;
    }

    @Override
    public SpendingAnalysis advise(SpendingAnalysisRequest request) {
        String rawResponse;
        try {
            rawResponse = chatModel.chat(buildPrompt(request));
        } catch (RuntimeException exception) {
            throw new AiIntegrationException("Failed to call Ollama for spending analysis. Make sure Ollama is running and the model is installed.", exception);
        }

        try {
            JsonNode root = objectMapper.readTree(extractJsonObject(rawResponse));
            String summary = root.path("summary").asText(buildFallbackSummary(request));
            List<String> insights = toStringList(root.path("insights"));
            List<String> recommendations = toStringList(root.path("recommendations"));

            if (insights.isEmpty()) {
                insights = buildFallbackInsights(request);
            }

            if (recommendations.isEmpty()) {
                recommendations = buildFallbackRecommendations(request);
            }

            return new SpendingAnalysis(summary, insights, recommendations);
        } catch (IOException exception) {
            return new SpendingAnalysis(
                    buildFallbackSummary(request),
                    buildFallbackInsights(request),
                    buildFallbackRecommendations(request)
            );
        }
    }

    private String buildPrompt(SpendingAnalysisRequest request) {
        return """
                You are a financial analysis assistant.

                Analyze the spending summary and return ONLY valid JSON with exactly these fields:
                - summary: string
                - insights: array of strings
                - recommendations: array of strings

                Rules:
                - No markdown.
                - No extra explanation outside JSON.
                - Keep insights practical and specific.
                - Mention the largest spending category if possible.
                - If there is very little data, say so clearly.

                Input:
                startDate: %s
                endDate: %s
                transactionCount: %d
                totalSpent: %s
                spendingByCategory: %s
                """.formatted(
                request.startDate(),
                request.endDate(),
                request.transactionCount(),
                request.totalSpent(),
                request.spendingByCategory()
        );
    }

    private String extractJsonObject(String rawResponse) {
        String cleaned = rawResponse.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7).trim();
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3).trim();
        }

        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3).trim();
        }

        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start == -1 || end == -1 || end < start) {
            throw new AiIntegrationException("Ollama did not return a JSON object for spending analysis: " + rawResponse);
        }

        return cleaned.substring(start, end + 1);
    }

    private List<String> toStringList(JsonNode node) {
        if (!node.isArray()) {
            return List.of();
        }

        List<String> values = new ArrayList<>();
        node.forEach(item -> {
            if (item.isTextual() && !item.asText().isBlank()) {
                values.add(item.asText());
            }
        });
        return values;
    }

    private String buildFallbackSummary(SpendingAnalysisRequest request) {
        if (request.transactionCount() == 0) {
            return "No transactions were found in the selected date range.";
        }

        return "You recorded %d transactions and spent a total of %s from %s to %s."
                .formatted(request.transactionCount(), request.totalSpent(), request.startDate(), request.endDate());
    }

    private List<String> buildFallbackInsights(SpendingAnalysisRequest request) {
        if (request.transactionCount() == 0) {
            return List.of("There is not enough spending data in the selected date range to generate insights.");
        }

        String topCategory = request.spendingByCategory()
                .entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("OTHER");

        return List.of(
                "Your largest spending category in this period is %s.".formatted(topCategory),
                "You spent a total of %s across %d transactions.".formatted(request.totalSpent(), request.transactionCount())
        );
    }

    private List<String> buildFallbackRecommendations(SpendingAnalysisRequest request) {
        if (request.transactionCount() == 0) {
            return List.of("Add more transaction data first, then run the analysis again.");
        }

        BigDecimal topCategoryAmount = request.spendingByCategory()
                .values()
                .stream()
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        return List.of(
                "Review the category where you spent %s the most and decide if part of it can be reduced.".formatted(topCategoryAmount),
                "Track this same date range again next week or next month to compare changes."
        );
    }
}
