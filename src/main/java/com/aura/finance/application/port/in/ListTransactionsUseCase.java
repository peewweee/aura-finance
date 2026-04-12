package com.aura.finance.application.port.in;

import com.aura.finance.domain.model.Transaction;

import java.util.List;

public interface ListTransactionsUseCase {

    List<Transaction> listTransactions(java.util.UUID sessionId);
}
