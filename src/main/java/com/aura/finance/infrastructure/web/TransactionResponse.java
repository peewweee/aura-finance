package com.aura.finance.infrastructure.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        String description,
        BigDecimal amount,
        String category,
        LocalDate transactionDate
) {
}
