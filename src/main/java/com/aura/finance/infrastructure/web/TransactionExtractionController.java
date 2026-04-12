package com.aura.finance.infrastructure.web;

import com.aura.finance.application.port.in.ConfirmExtractedTransactionsUseCase;
import com.aura.finance.application.port.in.ExtractTransactionsUseCase;
import com.aura.finance.domain.model.Transaction;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/transaction-extraction")
public class TransactionExtractionController {

    private final ExtractTransactionsUseCase extractTransactionsUseCase;
    private final ConfirmExtractedTransactionsUseCase confirmExtractedTransactionsUseCase;
    private final AnonymousSessionManager anonymousSessionManager;
    private final RequestRateLimiter requestRateLimiter;
    private final int extractionRequests;
    private final int extractionWindowMinutes;
    private final int confirmRequests;
    private final int confirmWindowMinutes;

    public TransactionExtractionController(
            ExtractTransactionsUseCase extractTransactionsUseCase,
            ConfirmExtractedTransactionsUseCase confirmExtractedTransactionsUseCase,
            AnonymousSessionManager anonymousSessionManager,
            RequestRateLimiter requestRateLimiter,
            @org.springframework.beans.factory.annotation.Value("${aura.rate-limit.extraction.requests}") int extractionRequests,
            @org.springframework.beans.factory.annotation.Value("${aura.rate-limit.extraction.window-minutes}") int extractionWindowMinutes,
            @org.springframework.beans.factory.annotation.Value("${aura.rate-limit.confirm.requests}") int confirmRequests,
            @org.springframework.beans.factory.annotation.Value("${aura.rate-limit.confirm.window-minutes}") int confirmWindowMinutes
    ) {
        this.extractTransactionsUseCase = extractTransactionsUseCase;
        this.confirmExtractedTransactionsUseCase = confirmExtractedTransactionsUseCase;
        this.anonymousSessionManager = anonymousSessionManager;
        this.requestRateLimiter = requestRateLimiter;
        this.extractionRequests = extractionRequests;
        this.extractionWindowMinutes = extractionWindowMinutes;
        this.confirmRequests = confirmRequests;
        this.confirmWindowMinutes = confirmWindowMinutes;
    }

    @PostMapping
    public List<ExtractedTransactionResponse> extractTransactions(
            @Valid @RequestBody ExtractTransactionsRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        java.util.UUID sessionId = anonymousSessionManager.resolveSessionId(httpRequest, httpResponse);
        requestRateLimiter.assertAllowed(
                "transaction-extraction",
                sessionId,
                httpRequest,
                extractionRequests,
                Duration.ofMinutes(extractionWindowMinutes)
        );
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
            @Valid @RequestBody ConfirmExtractedTransactionsRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        java.util.UUID sessionId = anonymousSessionManager.resolveSessionId(httpRequest, httpResponse);
        requestRateLimiter.assertAllowed(
                "transaction-confirm",
                sessionId,
                httpRequest,
                confirmRequests,
                Duration.ofMinutes(confirmWindowMinutes)
        );
        return confirmExtractedTransactionsUseCase.confirmTransactions(
                        new ConfirmExtractedTransactionsUseCase.ConfirmExtractedTransactionsCommand(
                                sessionId,
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
