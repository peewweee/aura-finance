package com.aura.finance.application.service;

import com.aura.finance.application.port.in.ListTransactionsUseCase;
import com.aura.finance.application.port.out.TransactionRepository;
import com.aura.finance.domain.model.Transaction;

import java.util.List;
import java.util.Objects;

public class TransactionQueryService implements ListTransactionsUseCase {

    private final TransactionRepository transactionRepository;

    public TransactionQueryService(TransactionRepository transactionRepository) {
        this.transactionRepository = Objects.requireNonNull(transactionRepository, "transactionRepository must not be null");
    }

    @Override
    public List<Transaction> listTransactions() {
        return transactionRepository.findAll();
    }
}
