package com.aura.finance.application.port.out;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public interface TransactionExtractor {

    List<ExtractedTransaction> extract(ExtractionRequest request);

    record ExtractionRequest(
            String rawText,
            LocalDate referenceDate
    ) {
        public ExtractionRequest {
            Objects.requireNonNull(rawText, "rawText must not be null");
            Objects.requireNonNull(referenceDate, "referenceDate must not be null");

            if (rawText.isBlank()) {
                throw new IllegalArgumentException("rawText must not be blank");
            }
        }
    }

    record ExtractedTransaction(
            String description,
            BigDecimal amount,
            String category,
            LocalDate transactionDate
    ) {
        public ExtractedTransaction {
            Objects.requireNonNull(description, "description must not be null");
            Objects.requireNonNull(amount, "amount must not be null");
            Objects.requireNonNull(category, "category must not be null");
            Objects.requireNonNull(transactionDate, "transactionDate must not be null");

            if (description.isBlank()) {
                throw new IllegalArgumentException("description must not be blank");
            }

            if (category.isBlank()) {
                throw new IllegalArgumentException("category must not be blank");
            }
        }
    }
}
