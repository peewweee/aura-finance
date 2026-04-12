package com.aura.finance.application.service;

import com.aura.finance.application.port.in.GetTransactionByIdUseCase;
import com.aura.finance.application.port.out.TransactionRepository;
import com.aura.finance.domain.model.Transaction;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class GetTransactionByIdService implements GetTransactionByIdUseCase {

    private final TransactionRepository transactionRepository;

    public GetTransactionByIdService(TransactionRepository transactionRepository) {
        this.transactionRepository = Objects.requireNonNull(transactionRepository, "transactionRepository must not be null");
    }

    @Override
    public Optional<Transaction> getTransactionById(UUID sessionId, UUID transactionId) {
        return transactionRepository.findByIdAndSessionId(transactionId, sessionId);
    }
}
