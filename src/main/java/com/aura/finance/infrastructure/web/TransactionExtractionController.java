package com.aura.finance.infrastructure.web;

import com.aura.finance.application.port.in.ConfirmExtractedTransactionsUseCase;
import com.aura.finance.application.port.in.ExtractTransactionsUseCase;
import com.aura.finance.domain.model.Transaction;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

    private final ExtractTransactionsUseCase extractTransactionsUseCase;
    private final ConfirmExtractedTransactionsUseCase confirmExtractedTransactionsUseCase;

    public TransactionExtractionController(
            ExtractTransactionsUseCase extractTransactionsUseCase,
            ConfirmExtractedTransactionsUseCase confirmExtractedTransactionsUseCase
    ) {
        this.extractTransactionsUseCase = extractTransactionsUseCase;
        this.confirmExtractedTransactionsUseCase = confirmExtractedTransactionsUseCase;
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

    @PostMapping("/confirm")
    public List<TransactionResponse> confirmTransactions(
            @Valid @RequestBody ConfirmExtractedTransactionsRequest request
    ) {
        return confirmExtractedTransactionsUseCase.confirmTransactions(
                        new ConfirmExtractedTransactionsUseCase.ConfirmExtractedTransactionsCommand(
                                request.transactions()
                                        .stream()
                                        .map(transaction -> new ConfirmExtractedTransactionsUseCase.ConfirmedTransaction(
                                                transaction.description(),
                                                transaction.amount(),
                                                transaction.category(),
                                                transaction.transactionDate()
                                        ))
                                        .toList()
                        )
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private TransactionResponse toResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.id(),
                transaction.description(),
                transaction.amount(),
                transaction.category(),
                transaction.transactionDate()
        );
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

    public record ConfirmExtractedTransactionsRequest(
            @NotEmpty
            @Valid
            List<ConfirmExtractedTransactionItem> transactions
    ) {
    }

    public record ConfirmExtractedTransactionItem(
            @NotBlank
            @Size(max = 255)
            String description,

            @NotNull
            @DecimalMin(value = "0.01")
            BigDecimal amount,

            @NotBlank
            @Size(max = 100)
            String category,

            @NotNull
            LocalDate transactionDate
    ) {
    }
}
