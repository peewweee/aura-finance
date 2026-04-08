package com.aura.finance.infrastructure.ai;

import com.aura.finance.application.port.out.TransactionExtractor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.ollama.OllamaChatModel;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.List;
import java.util.Objects;

public class OllamaTransactionExtractor implements TransactionExtractor {

    private static final TypeReference<List<OllamaExtractedTransaction>> RESPONSE_TYPE = new TypeReference<>() {
    };

    private final OllamaChatModel chatModel;
    private final ObjectMapper objectMapper;

    public OllamaTransactionExtractor(String baseUrl, String modelName, ObjectMapper objectMapper) {
        this.chatModel = OllamaChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(modelName)
                .temperature(0.0)
                .timeout(Duration.ofSeconds(60))
                .build();
        this.objectMapper = objectMapper;
    }

    @Override
    public List<ExtractedTransaction> extract(ExtractionRequest request) {
        String rawResponse;
        try {
            rawResponse = chatModel.chat(buildPrompt(request));
        } catch (RuntimeException exception) {
            throw new AiIntegrationException("Failed to call Ollama. Make sure Ollama is running and the model is installed.", exception);
        }

        String json = extractJsonArray(stripCodeFences(rawResponse));

        try {
            return objectMapper.readValue(json, RESPONSE_TYPE)
                    .stream()
                    .map(item -> toExtractedTransaction(item, request.referenceDate()))
                    .filter(Objects::nonNull)
                    .toList();
        } catch (IOException exception) {
            List<ExtractedTransaction> fallbackTransactions = fallbackExtractFromRawResponse(rawResponse, request.referenceDate());
            if (!fallbackTransactions.isEmpty()) {
                return fallbackTransactions;
            }

            throw new AiIntegrationException("Ollama returned a response that is not valid transaction JSON: " + rawResponse, exception);
        }
    }

    private String buildPrompt(ExtractionRequest request) {
        return """
                You are a financial transaction extraction engine.
                Extract every spending, income, savings, or bill-payment event from messy user text.

                Return ONLY valid JSON.
                Return a JSON array.
                Each item must have exactly these fields:
                - description: string
                - amount: number
                - category: string
                - transactionDate: string in ISO-8601 format (yyyy-MM-dd)

                Rules:
                - Do not include markdown.
                - Do not include explanation.
                - If no transactions are found, return [].
                - Use the reference date when the text implies "today", "kanina", "earlier", or does not specify a date.
                - If one sentence contains multiple transactions joined by commas, "and", "then", "tapos", "plus", or "&", split them into separate array items.
                - Ignore non-financial chatter.
                - Description must be short and specific.
                - Amount must be numeric only, no currency symbols or commas.
                - If the text says "spent", "paid", "bought", "ordered", "fare", "top up", "load", "tuition", "rent", "bill", "deposit", "salary", "allowance", treat it as a financial transaction when an amount is present.
                - If the amount is ambiguous, estimated, incomplete, or missing, skip that transaction.
                - If the date is explicit and clear, use it.
                - If the date is relative or missing, use the reference date.
                - If the date is conflicting or too ambiguous to resolve safely, skip that transaction.
                - transactionDate must always be yyyy-MM-dd.
                - category must be one of exactly these uppercase labels:
                  FOOD, TRANSPORT, SHOPPING, BILLS, HEALTH, EDUCATION, ENTERTAINMENT, SAVINGS, INCOME, GROCERIES, UTILITIES, OTHER

                Category guidance:
                - meals, coffee, snacks, restaurant, milk tea -> FOOD
                - jeep, tricycle, bus, mrt, lrt, grab, taxi, fare, gas -> TRANSPORT
                - groceries, supermarket, market, pantry items -> GROCERIES
                - internet, electricity, water, phone load -> UTILITIES
                - rent, tuition, loan payment, subscriptions, insurance -> BILLS
                - medicine, clinic, hospital -> HEALTH
                - school, books, course, tuition -> EDUCATION
                - movie, game, concert, leisure -> ENTERTAINMENT
                - clothes, gadgets, online shopping, accessories -> SHOPPING
                - salary, wages, allowance, freelance payment -> INCOME
                - savings deposit, emergency fund deposit, investment contribution -> SAVINGS

                Example output:
                [
                  {
                    "description": "Coffee",
                    "amount": 150.00,
                    "category": "FOOD",
                    "transactionDate": "2026-04-08"
                  }
                ]

                Example:
                Text: "Bought coffee for 150 and rode a jeep for 13"
                Output:
                [
                  {
                    "description": "Coffee",
                    "amount": 150.00,
                    "category": "FOOD",
                    "transactionDate": "2026-04-08"
                  },
                  {
                    "description": "Jeep fare",
                    "amount": 13.00,
                    "category": "TRANSPORT",
                    "transactionDate": "2026-04-08"
                  }
                ]

                Example:
                Text: "Spent around 200 to 300 on snacks maybe last week"
                Output:
                []

                Reference date: %s
                User text:
                %s
                """.formatted(request.referenceDate(), request.rawText());
    }

    private String stripCodeFences(String rawResponse) {
        String cleaned = rawResponse.trim();

        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7).trim();
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3).trim();
        }

        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3).trim();
        }

        return cleaned;
    }

    private String extractJsonArray(String response) {
        int start = response.indexOf('[');
        int end = response.lastIndexOf(']');

        if (start == -1 || end == -1 || end < start) {
            throw new AiIntegrationException("Ollama did not return a JSON array: " + response);
        }

        return response.substring(start, end + 1);
    }

    private String normalizeDescription(String description) {
        return description == null ? "" : description.trim();
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        if (amount == null) {
            return null;
        }

        if (amount.signum() <= 0) {
            return null;
        }

        return amount;
    }

    private LocalDate normalizeTransactionDate(String transactionDate, LocalDate referenceDate) {
        if (transactionDate == null) {
            return referenceDate;
        }

        String normalized = transactionDate.trim();
        if (normalized.isBlank() || normalized.equalsIgnoreCase("null")) {
            return referenceDate;
        }

        try {
            return LocalDate.parse(normalized);
        } catch (DateTimeParseException exception) {
            return referenceDate;
        }
    }

    private String normalizeCategory(String category, String description) {
        String normalizedCategory = category == null ? "" : category.trim().toUpperCase(Locale.ROOT);
        String normalizedDescription = description == null ? "" : description.trim().toLowerCase(Locale.ROOT);

        return switch (normalizedCategory) {
            case "FOOD", "MEALS", "DINING", "DRINKS", "BEVERAGES", "SNACKS" -> "FOOD";
            case "TRANSPORT", "TRANSPORTATION", "COMMUTE", "TRAVEL", "FARE" -> "TRANSPORT";
            case "SHOPPING", "RETAIL", "PURCHASES", "PURCHASE" -> "SHOPPING";
            case "BILLS", "BILL", "PAYMENT", "PAYMENTS", "RENT", "SUBSCRIPTIONS" -> "BILLS";
            case "HEALTH", "MEDICAL", "MEDICINE" -> "HEALTH";
            case "EDUCATION", "SCHOOL", "TUITION" -> "EDUCATION";
            case "ENTERTAINMENT", "LEISURE" -> "ENTERTAINMENT";
            case "SAVINGS", "INVESTMENT", "INVESTMENTS" -> "SAVINGS";
            case "INCOME", "SALARY", "ALLOWANCE", "EARNINGS" -> "INCOME";
            case "GROCERIES", "GROCERY" -> "GROCERIES";
            case "UTILITIES", "UTILITY", "LOAD" -> "UTILITIES";
            case "OTHER" -> inferCategoryFromDescription(normalizedDescription);
            default -> {
                if (!normalizedCategory.isBlank()) {
                    yield inferCategoryFromDescription(normalizedDescription);
                }
                yield inferCategoryFromDescription(normalizedDescription);
            }
        };
    }

    private String inferCategoryFromDescription(String description) {
        if (description.contains("coffee") || description.contains("meal") || description.contains("lunch")
                || description.contains("dinner") || description.contains("breakfast") || description.contains("snack")
                || description.contains("milk tea") || description.contains("restaurant") || description.contains("food")) {
            return "FOOD";
        }

        if (description.contains("jeep") || description.contains("tricycle") || description.contains("bus")
                || description.contains("mrt") || description.contains("lrt") || description.contains("grab")
                || description.contains("taxi") || description.contains("fare") || description.contains("gas")) {
            return "TRANSPORT";
        }

        if (description.contains("grocery") || description.contains("supermarket") || description.contains("market")) {
            return "GROCERIES";
        }

        if (description.contains("electric") || description.contains("water bill") || description.contains("internet")
                || description.contains("wifi") || description.contains("phone load") || description.contains("load")) {
            return "UTILITIES";
        }

        if (description.contains("rent") || description.contains("loan") || description.contains("insurance")
                || description.contains("subscription") || description.contains("tuition")) {
            return "BILLS";
        }

        if (description.contains("medicine") || description.contains("clinic") || description.contains("hospital")) {
            return "HEALTH";
        }

        if (description.contains("book") || description.contains("course") || description.contains("school")) {
            return "EDUCATION";
        }

        if (description.contains("movie") || description.contains("game") || description.contains("concert")) {
            return "ENTERTAINMENT";
        }

        if (description.contains("salary") || description.contains("allowance") || description.contains("pay")) {
            return "INCOME";
        }

        if (description.contains("savings") || description.contains("emergency fund") || description.contains("investment")) {
            return "SAVINGS";
        }

        if (description.contains("shop") || description.contains("shopee") || description.contains("lazada")
                || description.contains("clothes") || description.contains("gadget")) {
            return "SHOPPING";
        }

        return "OTHER";
    }

    private List<ExtractedTransaction> fallbackExtractFromRawResponse(String rawResponse, LocalDate referenceDate) {
        String normalized = rawResponse == null ? "" : rawResponse.toLowerCase(Locale.ROOT);

        if (normalized.contains("no transactions are found")
                || normalized.contains("no transaction found")
                || normalized.contains("does not contain any specific transaction details")
                || normalized.contains("return []")
                || normalized.contains("[]")) {
            return List.of();
        }

        return List.of();
    }

    private ExtractedTransaction toExtractedTransaction(
            OllamaExtractedTransaction item,
            LocalDate referenceDate
    ) {
        String description = normalizeDescription(item.description());
        BigDecimal amount = normalizeAmount(item.amount());
        LocalDate transactionDate = normalizeTransactionDate(item.transactionDate(), referenceDate);

        if (description.isBlank() || amount == null) {
            return null;
        }

        return new ExtractedTransaction(
                description,
                amount,
                normalizeCategory(item.category(), item.description()),
                transactionDate
        );
    }

    private record OllamaExtractedTransaction(
            String description,
            java.math.BigDecimal amount,
            String category,
            String transactionDate
    ) {
    }
}
