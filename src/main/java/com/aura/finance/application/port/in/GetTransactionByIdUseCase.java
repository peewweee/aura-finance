package com.aura.finance.application.port.in;

import com.aura.finance.domain.model.Transaction;

import java.util.Optional;
import java.util.UUID;

public interface GetTransactionByIdUseCase {

    Optional<Transaction> getTransactionById(UUID transactionId);
}
