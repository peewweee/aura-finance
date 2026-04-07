package com.aura.finance.application.service;

import com.aura.finance.application.port.in.ExtractTransactionsUseCase;
import com.aura.finance.application.port.out.TransactionExtractor;

import java.util.List;
import java.util.Objects;

public class ExtractTransactionsService implements ExtractTransactionsUseCase {

    private final TransactionExtractor transactionExtractor;

    public ExtractTransactionsService(TransactionExtractor transactionExtractor) {
        this.transactionExtractor = Objects.requireNonNull(transactionExtractor, "transactionExtractor must not be null");
    }

    @Override
    public List<ProposedTransaction> extractTransactions(ExtractTransactionsCommand command) {
        return transactionExtractor.extract(
                        new TransactionExtractor.ExtractionRequest(
                                command.rawText(),
                                command.referenceDate()
                        )
                )
                .stream()
                .map(extractedTransaction -> new ProposedTransaction(
                        extractedTransaction.description(),
                        extractedTransaction.amount(),
                        extractedTransaction.category(),
                        extractedTransaction.transactionDate()
                ))
                .toList();
    }
}
