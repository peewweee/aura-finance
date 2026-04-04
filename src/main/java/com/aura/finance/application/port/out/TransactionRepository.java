package com.aura.finance.application.port.out;

import com.aura.finance.domain.model.Transaction;

import java.util.List;

public interface TransactionRepository {

    Transaction save(Transaction transaction);

    List<Transaction> findAll();
}
