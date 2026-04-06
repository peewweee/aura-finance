package com.aura.finance.infrastructure.persistence;

import com.aura.finance.domain.model.Transaction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
class JpaTransactionRepositoryAdapterTest {

    @Autowired
    private SpringDataTransactionRepository springDataTransactionRepository;

    @Test
    void shouldSaveAndLoadTransactionsUsingJpa() {
        JpaTransactionRepositoryAdapter transactionRepository =
                new JpaTransactionRepositoryAdapter(springDataTransactionRepository);

        Transaction transaction = new Transaction(
                UUID.randomUUID(),
                "Emergency fund deposit",
                new BigDecimal("5000.00"),
                "SAVINGS",
                LocalDate.of(2026, 4, 6)
        );

        transactionRepository.save(transaction);
        List<Transaction> transactions = transactionRepository.findAll();

        assertEquals(1, transactions.size());
        assertEquals("Emergency fund deposit", transactions.getFirst().description());
        assertEquals(new BigDecimal("5000.00"), transactions.getFirst().amount());
        assertEquals("SAVINGS", transactions.getFirst().category());
        assertEquals(LocalDate.of(2026, 4, 6), transactions.getFirst().transactionDate());
    }
}
