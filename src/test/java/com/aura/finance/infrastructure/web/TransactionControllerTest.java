package com.aura.finance.infrastructure.web;

import com.aura.finance.application.port.in.CreateTransactionUseCase;
import com.aura.finance.application.port.in.CreateTransactionUseCase.CreateTransactionCommand;
import com.aura.finance.application.port.in.GetTransactionByIdUseCase;
import com.aura.finance.application.port.in.ListTransactionsUseCase;
import com.aura.finance.domain.model.Transaction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CreateTransactionUseCase createTransactionUseCase;

    @MockBean
    private ListTransactionsUseCase listTransactionsUseCase;

    @MockBean
    private GetTransactionByIdUseCase getTransactionByIdUseCase;

    @Test
    void shouldCreateTransactionAndReturnLocationHeader() throws Exception {
        UUID transactionId = UUID.randomUUID();
        Transaction transaction = new Transaction(
                transactionId,
                "Emergency fund deposit",
                new BigDecimal("5000.00"),
                "SAVINGS",
                LocalDate.of(2026, 4, 6)
        );

        when(createTransactionUseCase.createTransaction(any(CreateTransactionCommand.class)))
                .thenReturn(transaction);

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "description": "Emergency fund deposit",
                                  "amount": 5000.00,
                                  "category": "SAVINGS",
                                  "transactionDate": "2026-04-06"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/transactions/" + transactionId))
                .andExpect(jsonPath("$.id").value(transactionId.toString()))
                .andExpect(jsonPath("$.description").value("Emergency fund deposit"))
                .andExpect(jsonPath("$.amount").value(5000.00))
                .andExpect(jsonPath("$.category").value("SAVINGS"))
                .andExpect(jsonPath("$.transactionDate").value("2026-04-06"));
    }

    @Test
    void shouldReturnValidationErrorsForInvalidCreateTransactionRequest() throws Exception {
        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "description": "",
                                  "amount": 0,
                                  "category": "",
                                  "transactionDate": "2030-01-01"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors.length()").value(4));
    }

    @Test
    void shouldReturnTransactionWhenIdExists() throws Exception {
        UUID transactionId = UUID.randomUUID();
        Transaction transaction = new Transaction(
                transactionId,
                "Emergency fund deposit",
                new BigDecimal("5000.00"),
                "SAVINGS",
                LocalDate.of(2026, 4, 6)
        );

        when(getTransactionByIdUseCase.getTransactionById(transactionId))
                .thenReturn(Optional.of(transaction));

        mockMvc.perform(get("/transactions/{transactionId}", transactionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(transactionId.toString()))
                .andExpect(jsonPath("$.description").value("Emergency fund deposit"))
                .andExpect(jsonPath("$.amount").value(5000.00))
                .andExpect(jsonPath("$.category").value("SAVINGS"))
                .andExpect(jsonPath("$.transactionDate").value("2026-04-06"));
    }

    @Test
    void shouldReturnNotFoundWhenIdDoesNotExist() throws Exception {
        UUID transactionId = UUID.randomUUID();

        when(getTransactionByIdUseCase.getTransactionById(transactionId))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/transactions/{transactionId}", transactionId))
                .andExpect(status().isNotFound());
    }
}
