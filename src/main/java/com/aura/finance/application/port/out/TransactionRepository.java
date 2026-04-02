package com.aura.finance.application.port.out;

import com.aura.finance.domain.model.Transaction;

public interface TransactionRepository {

    Transaction save(Transaction transaction);
}
