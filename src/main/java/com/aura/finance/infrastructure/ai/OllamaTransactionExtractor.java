package com.aura.finance.infrastructure.ai;

import com.aura.finance.application.port.out.TransactionExtractor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.ollama.OllamaChatModel;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

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
                    .map(item -> new ExtractedTransaction(
                            item.description(),
                            item.amount(),
                            item.category(),
                            item.transactionDate()
                    ))
                    .toList();
        } catch (IOException exception) {
            throw new AiIntegrationException("Ollama returned a response that is not valid transaction JSON: " + rawResponse, exception);
        }
    }

    private String buildPrompt(ExtractionRequest request) {
        return """
                Extract financial transactions from the user text.

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
                - Use the reference date when the text implies "today" or does not specify a date.
                - Categories should be short uppercase labels like FOOD, TRANSPORT, SHOPPING, BILLS, HEALTH, EDUCATION, ENTERTAINMENT, SAVINGS, INCOME, OTHER.

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

    private record OllamaExtractedTransaction(
            String description,
            java.math.BigDecimal amount,
            String category,
            java.time.LocalDate transactionDate
    ) {
    }
}
