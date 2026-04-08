package com.aura.finance.application.service;

import com.aura.finance.application.port.in.ExtractTransactionsUseCase.ExtractTransactionsCommand;
import com.aura.finance.application.port.in.ExtractTransactionsUseCase.ProposedTransaction;
import com.aura.finance.application.port.out.TransactionExtractor;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExtractTransactionsServiceTest {

    @Test
    void shouldMapExtractorOutputToProposedTransactions() {
        TransactionExtractor transactionExtractor = request -> List.of(
                new TransactionExtractor.ExtractedTransaction(
                        "Coffee",
                        new BigDecimal("150.00"),
                        "FOOD",
                        request.referenceDate()
                ),
                new TransactionExtractor.ExtractedTransaction(
                        "Jeep fare",
                        new BigDecimal("13.00"),
                        "TRANSPORT",
                        request.referenceDate()
                )
        );

        ExtractTransactionsService service = new ExtractTransactionsService(transactionExtractor);

        List<ProposedTransaction> result = service.extractTransactions(
                new ExtractTransactionsCommand(
                        "Bought coffee for 150 and rode a jeep for 13",
                        LocalDate.of(2026, 4, 8)
                )
        );

        assertEquals(2, result.size());
        assertEquals("Coffee", result.get(0).description());
        assertEquals(new BigDecimal("150.00"), result.get(0).amount());
        assertEquals("FOOD", result.get(0).category());
        assertEquals(LocalDate.of(2026, 4, 8), result.get(0).transactionDate());
        assertEquals("Jeep fare", result.get(1).description());
        assertEquals(new BigDecimal("13.00"), result.get(1).amount());
        assertEquals("TRANSPORT", result.get(1).category());
        assertEquals(LocalDate.of(2026, 4, 8), result.get(1).transactionDate());
    }

    @Test
    void shouldReturnEmptyListWhenExtractorFindsNoTransactions() {
        TransactionExtractor transactionExtractor = request -> List.of();
        ExtractTransactionsService service = new ExtractTransactionsService(transactionExtractor);

        List<ProposedTransaction> result = service.extractTransactions(
                new ExtractTransactionsCommand(
                        "Spent around 200 to 300 on snacks maybe last week",
                        LocalDate.of(2026, 4, 8)
                )
        );

        assertEquals(List.of(), result);
    }
}
