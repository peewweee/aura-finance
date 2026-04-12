package com.aura.finance.infrastructure.web;

import com.aura.finance.application.port.out.SpendingAnalysisAdvisor;
import com.aura.finance.application.port.out.TransactionExtractor;
import com.aura.finance.infrastructure.persistence.SpringDataTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockCookie;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuraDemoFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SpringDataTransactionRepository springDataTransactionRepository;

    @MockBean
    private TransactionExtractor transactionExtractor;

    @MockBean
    private SpendingAnalysisAdvisor spendingAnalysisAdvisor;

    @BeforeEach
    void clearDatabase() {
        springDataTransactionRepository.deleteAll();
    }

    @Test
    void shouldExtractConfirmAndAnalyzeTransactionsEndToEnd() throws Exception {
        when(transactionExtractor.extract(any(TransactionExtractor.ExtractionRequest.class)))
                .thenReturn(List.of(
                        new TransactionExtractor.ExtractedTransaction(
                                "Coffee",
                                new BigDecimal("150.00"),
                                "FOOD",
                                LocalDate.of(2026, 4, 9)
                        ),
                        new TransactionExtractor.ExtractedTransaction(
                                "Groceries",
                                new BigDecimal("1850.00"),
                                "GROCERIES",
                                LocalDate.of(2026, 4, 9)
                        )
                ));

        when(spendingAnalysisAdvisor.advise(any(SpendingAnalysisAdvisor.SpendingAnalysisRequest.class)))
                .thenReturn(new SpendingAnalysisAdvisor.SpendingAnalysis(
                        "Groceries dominate your spending in this period.",
                        List.of(
                                "Groceries are much higher than your other tracked categories.",
                                "Your spending is concentrated in only a few purchases."
                        ),
                        List.of(
                                "Review grocery planning before making additional discretionary purchases.",
                                "Track next week to confirm whether this pattern repeats."
                        )
                ));

        MvcResult extractResult = mockMvc.perform(post("/transaction-extraction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rawText": "Bought coffee for 150 and groceries for 1850",
                                  "referenceDate": "2026-04-09"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].description").value("Coffee"))
                .andExpect(jsonPath("$[1].description").value("Groceries"))
                .andReturn();

        jakarta.servlet.http.Cookie responseCookie = extractResult.getResponse()
                .getCookie(AnonymousSessionManager.SESSION_COOKIE_NAME);
        MockCookie sessionCookie = new MockCookie(responseCookie.getName(), responseCookie.getValue());

        mockMvc.perform(post("/transaction-extraction/confirm")
                        .cookie(sessionCookie)
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
                                      "description": "Groceries",
                                      "amount": 1850.00,
                                      "category": "GROCERIES",
                                      "transactionDate": "2026-04-09"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").isNotEmpty())
                .andExpect(jsonPath("$[1].id").isNotEmpty());

        mockMvc.perform(post("/spending-analysis")
                        .cookie(sessionCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startDate": "2026-04-01",
                                  "endDate": "2026-04-30"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionCount").value(2))
                .andExpect(jsonPath("$.totalSpent").value(2000.00))
                .andExpect(jsonPath("$.spendingByCategory.GROCERIES").value(1850.00))
                .andExpect(jsonPath("$.spendingByCategory.FOOD").value(150.00))
                .andExpect(jsonPath("$.summary").value("Groceries dominate your spending in this period."));
    }
}
