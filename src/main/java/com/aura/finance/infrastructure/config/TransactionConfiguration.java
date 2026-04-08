package com.aura.finance.infrastructure.config;

import com.aura.finance.application.port.in.CreateTransactionUseCase;
import com.aura.finance.application.port.in.ExtractTransactionsUseCase;
import com.aura.finance.application.port.in.GetTransactionByIdUseCase;
import com.aura.finance.application.port.in.ListTransactionsUseCase;
import com.aura.finance.application.port.out.TransactionExtractor;
import com.aura.finance.application.port.out.TransactionRepository;
import com.aura.finance.application.service.CreateTransactionService;
import com.aura.finance.application.service.ExtractTransactionsService;
import com.aura.finance.application.service.GetTransactionByIdService;
import com.aura.finance.application.service.TransactionQueryService;
import com.aura.finance.infrastructure.ai.FakeTransactionExtractor;
import com.aura.finance.infrastructure.persistence.JpaTransactionRepositoryAdapter;
import com.aura.finance.infrastructure.persistence.SpringDataTransactionRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TransactionConfiguration {

    @Bean
    public TransactionRepository transactionRepository(SpringDataTransactionRepository springDataTransactionRepository) {
        return new JpaTransactionRepositoryAdapter(springDataTransactionRepository);
    }

    @Bean
    public CreateTransactionUseCase createTransactionUseCase(TransactionRepository transactionRepository) {
        return new CreateTransactionService(transactionRepository);
    }

    @Bean
    public ListTransactionsUseCase listTransactionsUseCase(TransactionRepository transactionRepository) {
        return new TransactionQueryService(transactionRepository);
    }

    @Bean
    public GetTransactionByIdUseCase getTransactionByIdUseCase(TransactionRepository transactionRepository) {
        return new GetTransactionByIdService(transactionRepository);
    }

    @Bean
    public TransactionExtractor transactionExtractor() {
        return new FakeTransactionExtractor();
    }

    @Bean
    public ExtractTransactionsUseCase extractTransactionsUseCase(TransactionExtractor transactionExtractor) {
        return new ExtractTransactionsService(transactionExtractor);
    }
}
