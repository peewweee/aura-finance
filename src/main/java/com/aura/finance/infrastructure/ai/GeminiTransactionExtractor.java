package com.aura.finance.infrastructure.ai;

import com.aura.finance.application.port.out.TransactionExtractor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GeminiTransactionExtractor implements TransactionExtractor {

    private static final TypeReference<List<ModelExtractedTransaction>> RESPONSE_TYPE = new TypeReference<>() {
    };
    private static final Pattern SPLIT_PATTERN = Pattern.compile(
            "(?i)\\s*(?:,|\\band\\b|\\bat\\b|\\bthen\\b|\\btapos\\b|\\bplus\\b|&|\\btsaka\\b)\\s*"
    );
    private static final Pattern DESCRIPTION_AMOUNT_PATTERN = Pattern.compile(
            "(?i)^(?:nagbayad\\s+ng\\s+|spent\\s+|paid\\s+|bought\\s+|ordered\\s+|gastos\\s+sa\\s+|gastos\\s+|bayad\\s+sa\\s+|bayad\\s+|binili\\s+ang\\s+|binili\\s+si\\s+|binili\\s+|sakay\\s+ng\\s+|sakay\\s+|pamasahe\\s+sa\\s+|pamasahe\\s+)?(?<description>[\\p{L}][\\p{L}\\s-]{0,60}?)\\s+(?:for\\s+|na\\s+|worth\\s+)?(?:php\\s*|₱\\s*|pesos?\\s+)?(?<amount>\\d+(?:\\.\\d{1,2})?)\\b"
    );
    private static final Pattern AMOUNT_DESCRIPTION_PATTERN = Pattern.compile(
            "(?i)^(?:spent\\s+|paid\\s+|bought\\s+|ordered\\s+|gastos\\s+ng\\s+|gastos\\s+|bayad\\s+ng\\s+|bayad\\s+|binili\\s+)?(?:php\\s*|₱\\s*)?(?<amount>\\d+(?:\\.\\d{1,2})?)\\s*(?:pesos?)?\\s+(?<description>[\\p{L}][\\p{L}\\s-]{0,60})$"
    );
    private static final Pattern NOISE_PREFIX_PATTERN = Pattern.compile(
            "(?i)^(?:spent|paid|bought|ordered|gastos|bayad|binili|sakay|pamasahe|for|ng|sa|si|ang)\\s+"
    );
    private static final Pattern NOISE_SUFFIX_PATTERN = Pattern.compile(
            "(?i)\\s+(?:today|kanina|ngayon|earlier|yesterday|kahapon)$"
    );
    private static final Set<String> STOP_DESCRIPTIONS = Set.of(
            "today", "kanina", "ngayon", "earlier", "yesterday", "kahapon", "php", "peso", "pesos"
    );

    private final GeminiResponsesClient responsesClient;
    private final ObjectMapper objectMapper;
    private final AiResponseCache aiResponseCache;
    private final Duration cacheTtl;

    public GeminiTransactionExtractor(
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
    public List<ExtractedTransaction> extract(ExtractionRequest request) {
        String prompt = buildPrompt(request);
        String rawResponse;
        java.util.Optional<String> cachedResponse = aiResponseCache.get("extract", prompt);
        if (cachedResponse.isPresent()) {
            rawResponse = cachedResponse.get();
        } else {
            rawResponse = responsesClient.generateText(prompt, 0.0);
            aiResponseCache.put("extract", prompt, rawResponse, cacheTtl);
        }

        String cleanedResponse = stripCodeFences(rawResponse);

        try {
            String json = extractJsonArray(cleanedResponse);
            return objectMapper.readValue(json, RESPONSE_TYPE)
                    .stream()
                    .map(item -> toExtractedTransaction(item, request.referenceDate()))
                    .filter(Objects::nonNull)
                    .toList();
        } catch (RuntimeException | IOException exception) {
            List<ExtractedTransaction> fallbackTransactions = fallbackExtractTransactions(
                    request.rawText(),
                    rawResponse,
                    request.referenceDate()
            );
            if (!fallbackTransactions.isEmpty()) {
                return fallbackTransactions;
            }

            throw new AiIntegrationException("Hosted AI returned a response that is not valid transaction JSON: " + rawResponse, exception);
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
                - The user may write in English, Tagalog, or Taglish. Understand all three naturally.
                - Use the reference date when the text implies "today", "kanina", "ngayon", "today", "earlier", or does not specify a date.
                - If one sentence contains multiple transactions joined by commas, "and", "then", "tapos", "plus", "&", "at", or "tsaka", split them into separate array items.
                - Ignore non-financial chatter.
                - Description must be short and specific.
                - Amount must be numeric only, no currency symbols or commas.
                - If the text says "spent", "paid", "bought", "ordered", "fare", "top up", "load", "tuition", "rent", "bill", "deposit", "salary", "allowance", "gastos", "bayad", "binili", "sakay", "pamasahe", "hulog", or "sweldo", treat it as a financial transaction when an amount is present.
                - If the amount is ambiguous, estimated, incomplete, or missing, skip that transaction.
                - If the date is explicit and clear, use it.
                - If the date is relative or missing, use the reference date.
                - If the date is conflicting or too ambiguous to resolve safely, skip that transaction.
                - transactionDate must always be yyyy-MM-dd.
                - category must be one of exactly these uppercase labels:
                  FOOD, TRANSPORT, SHOPPING, BILLS, HEALTH, EDUCATION, ENTERTAINMENT, SAVINGS, INCOME, GROCERIES, UTILITIES, OTHER

                Category guidance:
                - meals, coffee, snacks, restaurant, milk tea, kape, pagkain, merienda -> FOOD
                - jeep, tricycle, bus, mrt, lrt, grab, taxi, fare, gas, pamasahe, sakay -> TRANSPORT
                - groceries, supermarket, market, pantry items, palengke -> GROCERIES
                - internet, electricity, water, phone load, kuryente, tubig -> UTILITIES
                - rent, tuition, loan payment, subscriptions, insurance, upa, renta -> BILLS
                - medicine, clinic, hospital, gamot -> HEALTH
                - school, books, course, tuition, eskwela -> EDUCATION
                - movie, game, concert, leisure, sine -> ENTERTAINMENT
                - clothes, gadgets, online shopping, accessories, damit -> SHOPPING
                - salary, wages, allowance, freelance payment, sweldo, sahod -> INCOME
                - savings deposit, emergency fund deposit, investment contribution, ipon -> SAVINGS

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

                Example:
                Text: "Kape 150 at jeep 13 today"
                Output:
                [
                  {
                    "description": "Kape",
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
            throw new AiIntegrationException("Hosted AI did not return a JSON array: " + response);
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

    private static String inferCategoryFromDescription(String description) {
        if (description.contains("coffee") || description.contains("meal") || description.contains("lunch")
                || description.contains("dinner") || description.contains("breakfast") || description.contains("snack")
                || description.contains("milk tea") || description.contains("restaurant") || description.contains("food")
                || description.contains("kape") || description.contains("pagkain") || description.contains("ulam")
                || description.contains("merienda") || description.contains("almusal") || description.contains("tanghalian")
                || description.contains("hapunan")) {
            return "FOOD";
        }

        if (description.contains("jeep") || description.contains("tricycle") || description.contains("bus")
                || description.contains("mrt") || description.contains("lrt") || description.contains("grab")
                || description.contains("taxi") || description.contains("fare") || description.contains("gas")
                || description.contains("pamasahe") || description.contains("sakay")) {
            return "TRANSPORT";
        }

        if (description.contains("grocery") || description.contains("supermarket") || description.contains("market")
                || description.contains("palengke")) {
            return "GROCERIES";
        }

        if (description.contains("electric") || description.contains("water bill") || description.contains("internet")
                || description.contains("wifi") || description.contains("phone load") || description.contains("load")
                || description.contains("kuryente") || description.contains("tubig")) {
            return "UTILITIES";
        }

        if (description.contains("rent") || description.contains("loan") || description.contains("insurance")
                || description.contains("subscription") || description.contains("tuition")
                || description.contains("upa") || description.contains("renta")) {
            return "BILLS";
        }

        if (description.contains("medicine") || description.contains("clinic") || description.contains("hospital")
                || description.contains("gamot")) {
            return "HEALTH";
        }

        if (description.contains("book") || description.contains("course") || description.contains("school")
                || description.contains("eskwela")) {
            return "EDUCATION";
        }

        if (description.contains("movie") || description.contains("game") || description.contains("concert")
                || description.contains("sine")) {
            return "ENTERTAINMENT";
        }

        if (description.contains("salary") || description.contains("allowance") || description.contains("pay")
                || description.contains("sweldo") || description.contains("sahod")) {
            return "INCOME";
        }

        if (description.contains("savings") || description.contains("emergency fund") || description.contains("investment")
                || description.contains("ipon")) {
            return "SAVINGS";
        }

        if (description.contains("shop") || description.contains("shopee") || description.contains("lazada")
                || description.contains("clothes") || description.contains("gadget")
                || description.contains("damit")) {
            return "SHOPPING";
        }

        return "OTHER";
    }

    private List<ExtractedTransaction> fallbackExtractTransactions(
            String rawText,
            String rawResponse,
            LocalDate referenceDate
    ) {
        List<ExtractedTransaction> parsedFromText = parseTransactionsFromText(rawText, referenceDate);
        if (!parsedFromText.isEmpty()) {
            return parsedFromText;
        }

        return fallbackExtractFromRawResponse(rawResponse, referenceDate);
    }

    static List<ExtractedTransaction> parseTransactionsFromText(String rawText, LocalDate referenceDate) {
        if (rawText == null || rawText.isBlank()) {
            return List.of();
        }

        List<ExtractedTransaction> extracted = new ArrayList<>();
        Map<String, ExtractedTransaction> deduplicated = new LinkedHashMap<>();

        for (String segment : splitIntoCandidateSegments(rawText)) {
            ParsedCandidate candidate = parseCandidateSegment(segment, referenceDate);
            if (candidate == null) {
                continue;
            }

            ExtractedTransaction transaction = new ExtractedTransaction(
                    candidate.description(),
                    candidate.amount(),
                    inferCategoryFromDescription(candidate.description().toLowerCase(Locale.ROOT)),
                    candidate.transactionDate()
            );
            deduplicated.putIfAbsent(
                    transaction.description().toLowerCase(Locale.ROOT)
                            + "|" + transaction.amount()
                            + "|" + transaction.transactionDate(),
                    transaction
            );
        }

        extracted.addAll(deduplicated.values());
        return extracted;
    }

    private static List<String> splitIntoCandidateSegments(String rawText) {
        String normalized = rawText
                .replace('\n', ' ')
                .replace('\r', ' ')
                .replace(';', ',');

        String[] parts = SPLIT_PATTERN.split(normalized);
        List<String> segments = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part == null ? "" : part.trim();
            if (!trimmed.isBlank()) {
                segments.add(trimmed);
            }
        }
        return segments;
    }

    private static ParsedCandidate parseCandidateSegment(String segment, LocalDate referenceDate) {
        String normalized = segment
                .replaceAll("(?i)\\bpesos?\\b", " pesos ")
                .replaceAll("[.!?]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        if (normalized.isBlank()) {
            return null;
        }

        LocalDate transactionDate = inferTransactionDate(normalized, referenceDate);
        String withoutDateWords = NOISE_SUFFIX_PATTERN.matcher(normalized).replaceFirst("").trim();

        Matcher descriptionAmountMatcher = DESCRIPTION_AMOUNT_PATTERN.matcher(withoutDateWords);
        if (descriptionAmountMatcher.find()) {
            return buildCandidate(
                    descriptionAmountMatcher.group("description"),
                    descriptionAmountMatcher.group("amount"),
                    transactionDate
            );
        }

        Matcher amountDescriptionMatcher = AMOUNT_DESCRIPTION_PATTERN.matcher(withoutDateWords);
        if (amountDescriptionMatcher.find()) {
            return buildCandidate(
                    amountDescriptionMatcher.group("description"),
                    amountDescriptionMatcher.group("amount"),
                    transactionDate
            );
        }

        return null;
    }

    private static ParsedCandidate buildCandidate(String rawDescription, String rawAmount, LocalDate transactionDate) {
        String description = sanitizeDescription(rawDescription);
        if (description.isBlank() || STOP_DESCRIPTIONS.contains(description.toLowerCase(Locale.ROOT))) {
            return null;
        }

        BigDecimal amount;
        try {
            amount = new BigDecimal(rawAmount);
        } catch (NumberFormatException exception) {
            return null;
        }

        if (amount.signum() <= 0) {
            return null;
        }

        return new ParsedCandidate(description, amount, transactionDate);
    }

    private static String sanitizeDescription(String rawDescription) {
        if (rawDescription == null) {
            return "";
        }

        String description = rawDescription.trim()
                .replaceAll("(?i)\\bpesos?\\b", "")
                .replaceAll("(?i)\\bphp\\b", "")
                .replaceAll("₱", "")
                .replaceAll("\\s+", " ");

        while (true) {
            String updated = NOISE_PREFIX_PATTERN.matcher(description).replaceFirst("").trim();
            if (updated.equals(description)) {
                break;
            }
            description = updated;
        }

        description = NOISE_SUFFIX_PATTERN.matcher(description).replaceFirst("").trim();
        return description;
    }

    private static LocalDate inferTransactionDate(String segment, LocalDate referenceDate) {
        String normalized = segment.toLowerCase(Locale.ROOT);
        if (normalized.contains("yesterday") || normalized.contains("kahapon")) {
            return referenceDate.minusDays(1);
        }
        return referenceDate;
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

        return parseTransactionsFromText(rawResponse, referenceDate);
    }

    private ExtractedTransaction toExtractedTransaction(
            ModelExtractedTransaction item,
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

    private record ModelExtractedTransaction(
            String description,
            java.math.BigDecimal amount,
            String category,
            String transactionDate
    ) {
    }

    private record ParsedCandidate(
            String description,
            BigDecimal amount,
            LocalDate transactionDate
    ) {
    }
}
