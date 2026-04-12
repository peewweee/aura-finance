package com.aura.finance.application.service;

import com.aura.finance.application.port.in.CreateTransactionUseCase.CreateTransactionCommand;
import com.aura.finance.domain.model.Transaction;
import com.aura.finance.infrastructure.persistence.InMemoryTransactionRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CreateTransactionServiceTest {

    @Test
    void shouldCreateAndSaveTransaction() {
        InMemoryTransactionRepository transactionRepository = new InMemoryTransactionRepository();
        CreateTransactionService createTransactionService = new CreateTransactionService(transactionRepository);

        CreateTransactionCommand command = new CreateTransactionCommand(
                null,
                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                "Laptop upgrade",
                new BigDecimal("45000.00"),
                "TECHNOLOGY",
                LocalDate.of(2026, 4, 3)
        );

        Transaction transaction = createTransactionService.createTransaction(command);

        assertNotNull(transaction.id());
        assertEquals(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"), transaction.sessionId());
        assertEquals("Laptop upgrade", transaction.description());
        assertEquals(new BigDecimal("45000.00"), transaction.amount());
        assertEquals("TECHNOLOGY", transaction.category());
        assertEquals(LocalDate.of(2026, 4, 3), transaction.transactionDate());
    }
}
