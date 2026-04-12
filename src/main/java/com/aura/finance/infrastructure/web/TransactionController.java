package com.aura.finance.infrastructure.web;

import com.aura.finance.application.port.in.CreateTransactionUseCase;
import com.aura.finance.application.port.in.GetTransactionByIdUseCase;
import com.aura.finance.application.port.in.ListTransactionsUseCase;
import com.aura.finance.domain.model.Transaction;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final CreateTransactionUseCase createTransactionUseCase;
    private final GetTransactionByIdUseCase getTransactionByIdUseCase;
    private final ListTransactionsUseCase listTransactionsUseCase;
    private final AnonymousSessionManager anonymousSessionManager;

    public TransactionController(
            CreateTransactionUseCase createTransactionUseCase,
            GetTransactionByIdUseCase getTransactionByIdUseCase,
            ListTransactionsUseCase listTransactionsUseCase,
            AnonymousSessionManager anonymousSessionManager
    ) {
        this.createTransactionUseCase = createTransactionUseCase;
        this.getTransactionByIdUseCase = getTransactionByIdUseCase;
        this.listTransactionsUseCase = listTransactionsUseCase;
        this.anonymousSessionManager = anonymousSessionManager;
    }

    @PostMapping
    public ResponseEntity<TransactionResponse> createTransaction(
            @Valid @RequestBody CreateTransactionRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        UUID sessionId = anonymousSessionManager.resolveSessionId(httpRequest, httpResponse);
        Transaction transaction = createTransactionUseCase.createTransaction(
                new CreateTransactionUseCase.CreateTransactionCommand(
                        null,
                        sessionId,
                        request.description(),
                        request.amount(),
                        request.category(),
                        request.transactionDate()
                )
        );

        TransactionResponse response = toResponse(transaction);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{transactionId}")
                .buildAndExpand(transaction.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public List<TransactionResponse> listTransactions(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        UUID sessionId = anonymousSessionManager.resolveSessionId(httpRequest, httpResponse);
        return listTransactionsUseCase.listTransactions(sessionId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionResponse> getTransactionById(
            @PathVariable UUID transactionId,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        UUID sessionId = anonymousSessionManager.resolveSessionId(httpRequest, httpResponse);
        return getTransactionByIdUseCase.getTransactionById(sessionId, transactionId)
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
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
}
