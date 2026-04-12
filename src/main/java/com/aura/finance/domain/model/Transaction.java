package com.aura.finance.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public record Transaction(
        UUID id,
        UUID sessionId,
        String description,
        BigDecimal amount,
        String category,
        LocalDate transactionDate
) {

    public Transaction {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        Objects.requireNonNull(description, "description must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(category, "category must not be null");
        Objects.requireNonNull(transactionDate, "transactionDate must not be null");
    }
}
