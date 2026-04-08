package com.aura.finance.application.port.in;

import com.aura.finance.domain.model.Transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public interface ConfirmExtractedTransactionsUseCase {

    List<Transaction> confirmTransactions(ConfirmExtractedTransactionsCommand command);

    record ConfirmExtractedTransactionsCommand(
            List<ConfirmedTransaction> transactions
    ) {
        public ConfirmExtractedTransactionsCommand {
            Objects.requireNonNull(transactions, "transactions must not be null");

            if (transactions.isEmpty()) {
                throw new IllegalArgumentException("transactions must not be empty");
            }
        }
    }

    record ConfirmedTransaction(
            String description,
            BigDecimal amount,
            String category,
            LocalDate transactionDate
    ) {
        public ConfirmedTransaction {
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
