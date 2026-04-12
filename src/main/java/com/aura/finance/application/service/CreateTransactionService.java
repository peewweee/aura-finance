package com.aura.finance.application.service;

import com.aura.finance.application.port.in.CreateTransactionUseCase;
import com.aura.finance.application.port.out.TransactionRepository;
import com.aura.finance.domain.model.Transaction;

import java.util.Objects;
import java.util.UUID;

public class CreateTransactionService implements CreateTransactionUseCase {

    private final TransactionRepository transactionRepository;

    public CreateTransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = Objects.requireNonNull(transactionRepository, "transactionRepository must not be null");
    }

    @Override
    public Transaction createTransaction(CreateTransactionCommand command) {
        UUID transactionId = command.id() != null ? command.id() : UUID.randomUUID();

        Transaction transaction = new Transaction(
                transactionId,
                command.sessionId(),
                command.description(),
                command.amount(),
                command.category(),
                command.transactionDate()
        );

        return transactionRepository.save(transaction);
    }
}
