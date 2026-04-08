package com.aura.finance.application.service;

import com.aura.finance.application.port.in.ConfirmExtractedTransactionsUseCase;
import com.aura.finance.application.port.out.TransactionRepository;
import com.aura.finance.domain.model.Transaction;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ConfirmExtractedTransactionsService implements ConfirmExtractedTransactionsUseCase {

    private final TransactionRepository transactionRepository;

    public ConfirmExtractedTransactionsService(TransactionRepository transactionRepository) {
        this.transactionRepository = Objects.requireNonNull(transactionRepository, "transactionRepository must not be null");
    }

    @Override
    public List<Transaction> confirmTransactions(ConfirmExtractedTransactionsCommand command) {
        return command.transactions()
                .stream()
                .map(confirmedTransaction -> new Transaction(
                        UUID.randomUUID(),
                        confirmedTransaction.description(),
                        confirmedTransaction.amount(),
                        confirmedTransaction.category(),
                        confirmedTransaction.transactionDate()
                ))
                .map(transactionRepository::save)
                .toList();
    }
}
