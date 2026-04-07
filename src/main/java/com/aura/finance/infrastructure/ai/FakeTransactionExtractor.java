package com.aura.finance.infrastructure.ai;

import com.aura.finance.application.port.out.TransactionExtractor;

import java.math.BigDecimal;
import java.util.List;

public class FakeTransactionExtractor implements TransactionExtractor {

    @Override
    public List<ExtractedTransaction> extract(ExtractionRequest request) {
        return List.of(
                new ExtractedTransaction(
                        "Coffee",
                        new BigDecimal("150.00"),
                        "FOOD",
                        request.referenceDate()
                ),
                new ExtractedTransaction(
                        "Jeep fare",
                        new BigDecimal("13.00"),
                        "TRANSPORT",
                        request.referenceDate()
                )
        );
    }
}
