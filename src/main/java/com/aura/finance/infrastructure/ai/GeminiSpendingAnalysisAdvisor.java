package com.aura.finance.infrastructure.ai;

import com.aura.finance.application.port.out.SpendingAnalysisAdvisor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GeminiSpendingAnalysisAdvisor implements SpendingAnalysisAdvisor {

    private final GeminiResponsesClient responsesClient;
    private final ObjectMapper objectMapper;
    private final AiResponseCache aiResponseCache;
    private final Duration cacheTtl;

    public GeminiSpendingAnalysisAdvisor(
            GeminiResponsesClient responsesClient,
            ObjectMapper objectMapper,
            AiResponseCache aiResponseCache,
            long cacheTtlMinutes
    ) {
        this.responsesClient = responsesClient;
        this.objectMapper = objectMapper;
        this.aiResponseCache = aiResponseCache;
        this.cacheTtl = Duration.ofMinutes(cacheTtlMinutes);
    }

    @Override
    public SpendingAnalysis advise(SpendingAnalysisRequest request) {
        String prompt = buildPrompt(request);
        String rawResponse;
        java.util.Optional<String> cachedResponse = aiResponseCache.get("analysis", prompt);
        if (cachedResponse.isPresent()) {
            rawResponse = cachedResponse.get();
        } else {
            rawResponse = responsesClient.generateText(prompt, 0.2);
            aiResponseCache.put("analysis", prompt, rawResponse, cacheTtl);
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
                - Even with a small dataset, still provide useful observations from the available numbers.
                - Only say data is insufficient when transactionCount is exactly 0.
                - If there are 1 or more transactions, identify the top spending category and compare it with the others.
                - Recommendations should be concrete and action-oriented, not generic.
                - This app uses Philippine peso. Never refer to dollars, USD, or "$".
                - When mentioning money, always format it as peso with comma separators, like ₱5,000.00.

                Input:
                startDate: %s
                endDate: %s
                transactionCount: %d
                totalSpent: %s
                spendingByCategory: %s
                
                Example valid response:
                {
                  "summary": "You spent most of your money on groceries in this period.",
                  "insights": [
                    "Groceries dominate your spending compared with other categories.",
                    "Food and transport are relatively small compared with groceries."
                  ],
                  "recommendations": [
                    "Review grocery spending and check if any items can be planned more efficiently.",
                    "Track whether this grocery-heavy pattern repeats next week or next month."
                  ]
                }
                """.formatted(
                request.startDate(),
                request.endDate(),
                request.transactionCount(),
                AiMoneyFormatter.formatPhp(request.totalSpent()),
                AiMoneyFormatter.formatPhpMapInline(request.spendingByCategory())
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
            throw new AiIntegrationException("Hosted AI did not return a JSON object for spending analysis: " + rawResponse);
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

        String topCategory = request.spendingByCategory()
                .entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("OTHER");

        return "You recorded %d transactions and spent a total of %s from %s to %s, with %s as your largest category."
                .formatted(
                        request.transactionCount(),
                        AiMoneyFormatter.formatPhp(request.totalSpent()),
                        request.startDate(),
                        request.endDate(),
                        topCategory
                );
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

        java.math.BigDecimal topCategoryAmount = request.spendingByCategory()
                .getOrDefault(topCategory, java.math.BigDecimal.ZERO);

        return List.of(
                "%s is your largest spending category at %s.".formatted(topCategory, AiMoneyFormatter.formatPhp(topCategoryAmount)),
                "You spent a total of %s across %d transactions in this date range."
                        .formatted(AiMoneyFormatter.formatPhp(request.totalSpent()), request.transactionCount())
        );
    }

    private List<String> buildFallbackRecommendations(SpendingAnalysisRequest request) {
        if (request.transactionCount() == 0) {
            return List.of("Add more transaction data first, then run the analysis again.");
        }

        java.math.BigDecimal topCategoryAmount = request.spendingByCategory()
                .values()
                .stream()
                .max(java.math.BigDecimal::compareTo)
                .orElse(java.math.BigDecimal.ZERO);

        return List.of(
                "Review the category where you spent %s the most and decide if part of it can be reduced."
                        .formatted(AiMoneyFormatter.formatPhp(topCategoryAmount)),
                "Use this result as a baseline and compare the same categories again in your next tracking period."
        );
    }
}
