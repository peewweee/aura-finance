package com.aura.finance.application.port.in;

import com.aura.finance.domain.model.Transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public interface CreateTransactionUseCase {

    Transaction createTransaction(CreateTransactionCommand command);

    record CreateTransactionCommand(
            UUID id,
            String description,
            BigDecimal amount,
            String category,
            LocalDate transactionDate
    ) {
    }
}
