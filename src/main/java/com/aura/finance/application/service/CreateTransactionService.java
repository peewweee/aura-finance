package com.aura.finance.application.service;

import com.aura.finance.application.port.in.CreateTransactionUseCase;
import com.aura.finance.domain.model.Transaction;

import java.util.UUID;

public class CreateTransactionService implements CreateTransactionUseCase {

    @Override
    public Transaction createTransaction(CreateTransactionCommand command) {
        UUID transactionId = command.id() != null ? command.id() : UUID.randomUUID();

        return new Transaction(
                transactionId,
                command.description(),
                command.amount(),
                command.category(),
                command.transactionDate()
        );
    }
}
