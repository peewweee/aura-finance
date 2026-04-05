package com.aura.finance.infrastructure.config;

import com.aura.finance.application.port.in.CreateTransactionUseCase;
import com.aura.finance.application.port.in.ListTransactionsUseCase;
import com.aura.finance.application.port.out.TransactionRepository;
import com.aura.finance.application.service.CreateTransactionService;
import com.aura.finance.application.service.TransactionQueryService;
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
}
