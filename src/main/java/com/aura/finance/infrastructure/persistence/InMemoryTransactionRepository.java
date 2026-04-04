package com.aura.finance.infrastructure.persistence;

import com.aura.finance.application.port.out.TransactionRepository;
import com.aura.finance.domain.model.Transaction;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryTransactionRepository implements TransactionRepository {

    private final Map<UUID, Transaction> storage = new ConcurrentHashMap<>();

    @Override
    public Transaction save(Transaction transaction) {
        storage.put(transaction.id(), transaction);
        return transaction;
    }

    @Override
    public List<Transaction> findAll() {
        return storage.values().stream().toList();
    }
}
