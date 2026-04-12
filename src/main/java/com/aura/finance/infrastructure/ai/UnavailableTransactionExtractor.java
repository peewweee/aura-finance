package com.aura.finance.infrastructure.ai;

import com.aura.finance.application.port.out.TransactionExtractor;

import java.util.List;

public class UnavailableTransactionExtractor implements TransactionExtractor {

    private final String message;

    public UnavailableTransactionExtractor(String message) {
        this.message = message;
    }

    @Override
    public List<ExtractedTransaction> extract(ExtractionRequest request) {
        throw new AiIntegrationException(message);
    }
}
