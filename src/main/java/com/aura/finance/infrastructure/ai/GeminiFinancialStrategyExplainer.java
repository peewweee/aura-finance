package com.aura.finance.infrastructure.ai;

import com.aura.finance.application.port.out.FinancialStrategyExplainer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class GeminiFinancialStrategyExplainer implements FinancialStrategyExplainer {

    private final GeminiResponsesClient responsesClient;
    private final ObjectMapper objectMapper;
    private final AiResponseCache aiResponseCache;
    private final Duration cacheTtl;

    public GeminiFinancialStrategyExplainer(
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
    public FinancialStrategyExplanation explain(FinancialStrategyRequest request) {
        String prompt = buildPrompt(request);
        String rawResponse;
        java.util.Optional<String> cachedResponse = aiResponseCache.get("strategy", prompt);
        if (cachedResponse.isPresent()) {
            rawResponse = cachedResponse.get();
        } else {
            rawResponse = responsesClient.generateText(prompt, 0.2);
            aiResponseCache.put("strategy", prompt, rawResponse, cacheTtl);
        }

        try {
            JsonNode root = objectMapper.readTree(extractJsonObject(rawResponse));
            String spendingInsight = root.path("spendingInsight").asText(buildFallbackInsight(request));
            List<String> cautionFlags = toStringList(root.path("cautionFlags"));
            String recommendationText = root.path("recommendationText").asText(buildFallbackRecommendation(request));

            if (cautionFlags.isEmpty()) {
                cautionFlags = buildFallbackCautionFlags(request);
            }

            return new FinancialStrategyExplanation(spendingInsight, cautionFlags, recommendationText);
        } catch (IOException exception) {
            return new FinancialStrategyExplanation(
                    buildFallbackInsight(request),
                    buildFallbackCautionFlags(request),
                    buildFallbackRecommendation(request)
            );
        }
    }

    private String buildPrompt(FinancialStrategyRequest request) {
        return """
                You are Aura, a personal finance strategy assistant.

                Return ONLY valid JSON with exactly these fields:
                - spendingInsight: string
                - cautionFlags: array of strings
                - recommendationText: string

                Rules:
                - No markdown.
                - No text outside JSON.
                - Use the spending summary to identify the dominant spending pattern.
                - If purchase simulation data exists, connect it to the recommendation.
                - cautionFlags should be short and practical.
                - recommendationText should be concise but actionable.
                - This app uses Philippine peso. Never refer to dollars, USD, or "$".
                - When mentioning money, always format it as peso with comma separators, like ₱5,000.00.

                Structured input:
                startDate: %s
                endDate: %s
                transactionCount: %d
                totalSpent: %s
                spendingByCategory: %s
                plannedPurchaseAmount: %s
                expectedMonthlyReturnRate: %s
                timeHorizonMonths: %s
                futureValueIfInvested: %s
                opportunityCost: %s
                """.formatted(
                request.startDate(),
                request.endDate(),
                request.transactionCount(),
                AiMoneyFormatter.formatPhp(request.totalSpent()),
                AiMoneyFormatter.formatPhpMapInline(request.spendingByCategory()),
                AiMoneyFormatter.formatPhp(request.plannedPurchaseAmount()),
                request.expectedMonthlyReturnRate(),
                request.timeHorizonMonths(),
                AiMoneyFormatter.formatPhp(request.futureValueIfInvested()),
                AiMoneyFormatter.formatPhp(request.opportunityCost())
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
            throw new AiIntegrationException("Hosted AI did not return a JSON object for financial strategy explanation: " + rawResponse);
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

    private String buildFallbackInsight(FinancialStrategyRequest request) {
        if (request.transactionCount() == 0) {
            return "There is no spending data in the selected range yet, so strategy advice is limited.";
        }

        String topCategory = request.spendingByCategory()
                .entrySet()
                .stream()
                .max(java.util.Map.Entry.comparingByValue())
                .map(java.util.Map.Entry::getKey)
                .orElse("OTHER");

        return "Your strongest spending signal in this period is %s, which appears to be the main driver of your total spending.".formatted(topCategory);
    }

    private List<String> buildFallbackCautionFlags(FinancialStrategyRequest request) {
        List<String> cautionFlags = new ArrayList<>();

        if (request.transactionCount() <= 3) {
            cautionFlags.add("Your dataset is still small, so patterns may change as you log more transactions.");
        }

        if (request.opportunityCost() != null && request.opportunityCost().signum() > 0) {
            cautionFlags.add(
                    "The planned purchase has a measurable long-term opportunity cost of %s if you skip investing that amount."
                            .formatted(AiMoneyFormatter.formatPhp(request.opportunityCost()))
            );
        }

        if (cautionFlags.isEmpty()) {
            cautionFlags.add("Review your highest spending category regularly to prevent quiet overspending.");
        }

        return cautionFlags;
    }

    private String buildFallbackRecommendation(FinancialStrategyRequest request) {
        if (request.opportunityCost() != null && request.futureValueIfInvested() != null) {
            return "Compare this purchase against your top spending category and the projected future value of investing the same amount before deciding.";
        }

        return "Focus first on the category consuming the most money, then track whether it improves over the next period.";
    }
}
