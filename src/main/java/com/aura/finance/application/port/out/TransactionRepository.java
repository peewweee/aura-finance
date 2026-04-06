package com.aura.finance.application.port.out;

import com.aura.finance.domain.model.Transaction;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository {

    Transaction save(Transaction transaction);

    List<Transaction> findAll();

    Optional<Transaction> findById(UUID transactionId);
}
