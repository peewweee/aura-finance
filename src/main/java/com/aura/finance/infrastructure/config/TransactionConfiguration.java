package com.aura.finance.infrastructure.config;

import com.aura.finance.application.port.in.CreateTransactionUseCase;
import com.aura.finance.application.port.out.TransactionRepository;
import com.aura.finance.application.service.CreateTransactionService;
import com.aura.finance.infrastructure.persistence.InMemoryTransactionRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TransactionConfiguration {

    @Bean
    public TransactionRepository transactionRepository() {
        return new InMemoryTransactionRepository();
    }

    @Bean
    public CreateTransactionUseCase createTransactionUseCase(TransactionRepository transactionRepository) {
        return new CreateTransactionService(transactionRepository);
    }
}
