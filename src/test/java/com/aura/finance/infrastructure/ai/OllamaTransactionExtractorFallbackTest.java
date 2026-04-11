package com.aura.finance.infrastructure.ai;

import com.aura.finance.application.port.out.TransactionExtractor;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OllamaTransactionExtractorFallbackTest {

    @Test
    void shouldParseSimpleTaglishTransactionsFromRawTextFallback() {
        List<TransactionExtractor.ExtractedTransaction> result = OllamaTransactionExtractor.parseTransactionsFromText(
                "Groceries 1850, kape 150, at jeep 13 today.",
                LocalDate.of(2026, 4, 12)
        );

        assertEquals(3, result.size());
        assertEquals("Groceries", result.get(0).description());
        assertEquals(new BigDecimal("1850"), result.get(0).amount());
        assertEquals("GROCERIES", result.get(0).category());
        assertEquals(LocalDate.of(2026, 4, 12), result.get(0).transactionDate());

        assertEquals("kape", result.get(1).description());
        assertEquals(new BigDecimal("150"), result.get(1).amount());
        assertEquals("FOOD", result.get(1).category());

        assertEquals("jeep", result.get(2).description());
        assertEquals(new BigDecimal("13"), result.get(2).amount());
        assertEquals("TRANSPORT", result.get(2).category());
    }

    @Test
    void shouldParseAmountBeforeDescriptionWithYesterday() {
        List<TransactionExtractor.ExtractedTransaction> result = OllamaTransactionExtractor.parseTransactionsFromText(
                "bought 99 pesos chicken yesterday",
                LocalDate.of(2026, 4, 12)
        );

        assertEquals(1, result.size());
        assertEquals("chicken", result.getFirst().description());
        assertEquals(new BigDecimal("99"), result.getFirst().amount());
        assertEquals("FOOD", result.getFirst().category());
        assertEquals(LocalDate.of(2026, 4, 11), result.getFirst().transactionDate());
    }
}
