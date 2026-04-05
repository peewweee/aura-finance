package com.aura.finance.infrastructure.web;

import com.aura.finance.application.port.in.CreateTransactionUseCase;
import com.aura.finance.application.port.in.ListTransactionsUseCase;
import com.aura.finance.domain.model.Transaction;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final CreateTransactionUseCase createTransactionUseCase;
    private final ListTransactionsUseCase listTransactionsUseCase;

    public TransactionController(
            CreateTransactionUseCase createTransactionUseCase,
            ListTransactionsUseCase listTransactionsUseCase
    ) {
        this.createTransactionUseCase = createTransactionUseCase;
        this.listTransactionsUseCase = listTransactionsUseCase;
    }

    @PostMapping
    public TransactionResponse createTransaction(@RequestBody CreateTransactionRequest request) {
        Transaction transaction = createTransactionUseCase.createTransaction(
                new CreateTransactionUseCase.CreateTransactionCommand(
                        null,
                        request.description(),
                        request.amount(),
                        request.category(),
                        request.transactionDate()
                )
        );

        return new TransactionResponse(
                transaction.id(),
                transaction.description(),
                transaction.amount(),
                transaction.category(),
                transaction.transactionDate()
        );
    }

    @GetMapping
    public List<TransactionResponse> listTransactions() {
        return listTransactionsUseCase.listTransactions()
                .stream()
                .map(transaction -> new TransactionResponse(
                        transaction.id(),
                        transaction.description(),
                        transaction.amount(),
                        transaction.category(),
                        transaction.transactionDate()
                ))
                .toList();
    }

    public record CreateTransactionRequest(
            String description,
            BigDecimal amount,
            String category,
            LocalDate transactionDate
    ) {
    }

    public record TransactionResponse(
            UUID id,
            String description,
            BigDecimal amount,
            String category,
            LocalDate transactionDate
    ) {
    }
}
