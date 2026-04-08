package com.aura.finance.infrastructure.web;

import com.aura.finance.application.port.in.ExtractTransactionsUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/transaction-extraction")
public class TransactionExtractionController {
objectm
    private final ExtractTransactionsUseCase extractTransactionsUseCase;

    public TransactionExtractionController(ExtractTransactionsUseCase extractTransactionsUseCase) {
        this.extractTransactionsUseCase = extractTransactionsUseCase;
    }

    @PostMapping
    public List<ExtractedTransactionResponse> extractTransactions(
            @Valid @RequestBody ExtractTransactionsRequest request
    ) {
        return extractTransactionsUseCase.extractTransactions(
                        new ExtractTransactionsUseCase.ExtractTransactionsCommand(
                                request.rawText(),
                                request.referenceDate()
                        )
                )
                .stream()
                .map(proposedTransaction -> new ExtractedTransactionResponse(
                        proposedTransaction.description(),
                        proposedTransaction.amount(),
                        proposedTransaction.category(),
                        proposedTransaction.transactionDate()
                ))
                .toList();
    }

    public record ExtractTransactionsRequest(
            @NotBlank
            String rawText,

            @NotNull
            LocalDate referenceDate
    ) {
    }

    public record ExtractedTransactionResponse(
            String description,
            BigDecimal amount,
            String category,
            LocalDate transactionDate
    ) {
    }
}
