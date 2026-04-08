package com.aura.finance.infrastructure.web;

import com.aura.finance.application.port.in.ConfirmExtractedTransactionsUseCase;
import com.aura.finance.application.port.in.ExtractTransactionsUseCase;
import com.aura.finance.domain.model.Transaction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionExtractionController.class)
class TransactionExtractionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExtractTransactionsUseCase extractTransactionsUseCase;

    @MockBean
    private ConfirmExtractedTransactionsUseCase confirmExtractedTransactionsUseCase;

    @Test
    void shouldReturnExtractedTransactions() throws Exception {
        when(extractTransactionsUseCase.extractTransactions(any(ExtractTransactionsUseCase.ExtractTransactionsCommand.class)))
                .thenReturn(List.of(
                        new ExtractTransactionsUseCase.ProposedTransaction(
                                "Coffee",
                                new BigDecimal("150.00"),
                                "FOOD",
                                LocalDate.of(2026, 4, 9)
                        ),
                        new ExtractTransactionsUseCase.ProposedTransaction(
                                "Jeep fare",
                                new BigDecimal("13.00"),
                                "TRANSPORT",
                                LocalDate.of(2026, 4, 9)
                        )
                ));

        mockMvc.perform(post("/transaction-extraction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rawText": "Bought coffee for 150 and rode a jeep for 13",
                                  "referenceDate": "2026-04-09"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].description").value("Coffee"))
                .andExpect(jsonPath("$[0].amount").value(150.00))
                .andExpect(jsonPath("$[0].category").value("FOOD"))
                .andExpect(jsonPath("$[1].description").value("Jeep fare"))
                .andExpect(jsonPath("$[1].amount").value(13.00))
                .andExpect(jsonPath("$[1].category").value("TRANSPORT"));
    }

    @Test
    void shouldSaveConfirmedTransactions() throws Exception {
        when(confirmExtractedTransactionsUseCase.confirmTransactions(any(ConfirmExtractedTransactionsUseCase.ConfirmExtractedTransactionsCommand.class)))
                .thenReturn(List.of(
                        new Transaction(
                                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                                "Coffee",
                                new BigDecimal("150.00"),
                                "FOOD",
                                LocalDate.of(2026, 4, 9)
                        ),
                        new Transaction(
                                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                                "Jeep fare",
                                new BigDecimal("13.00"),
                                "TRANSPORT",
                                LocalDate.of(2026, 4, 9)
                        )
                ));

        mockMvc.perform(post("/transaction-extraction/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "transactions": [
                                    {
                                      "description": "Coffee",
                                      "amount": 150.00,
                                      "category": "FOOD",
                                      "transactionDate": "2026-04-09"
                                    },
                                    {
                                      "description": "Jeep fare",
                                      "amount": 13.00,
                                      "category": "TRANSPORT",
                                      "transactionDate": "2026-04-09"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("11111111-1111-1111-1111-111111111111"))
                .andExpect(jsonPath("$[0].description").value("Coffee"))
                .andExpect(jsonPath("$[1].id").value("22222222-2222-2222-2222-222222222222"))
                .andExpect(jsonPath("$[1].description").value("Jeep fare"));
    }
}
