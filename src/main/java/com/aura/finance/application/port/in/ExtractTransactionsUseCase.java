package com.aura.finance.application.port.in;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public interface ExtractTransactionsUseCase {

    List<ProposedTransaction> extractTransactions(ExtractTransactionsCommand command);

    record ExtractTransactionsCommand(
            String rawText,
            LocalDate referenceDate
    ) {
        public ExtractTransactionsCommand {
            Objects.requireNonNull(rawText, "rawText must not be null");
            Objects.requireNonNull(referenceDate, "referenceDate must not be null");

            if (rawText.isBlank()) {
                throw new IllegalArgumentException("rawText must not be blank");
            }
        }
    }

    record ProposedTransaction(
            String description,
            BigDecimal amount,
            String category,
            LocalDate transactionDate
    ) {
    }
}
