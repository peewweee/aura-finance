package com.aura.finance.infrastructure.persistence;

import com.aura.finance.application.port.out.TransactionRepository;
import com.aura.finance.domain.model.Transaction;

import java.util.List;

public class JpaTransactionRepositoryAdapter implements TransactionRepository {

    private final SpringDataTransactionRepository springDataTransactionRepository;

    public JpaTransactionRepositoryAdapter(SpringDataTransactionRepository springDataTransactionRepository) {
        this.springDataTransactionRepository = springDataTransactionRepository;
    }

    @Override
    public Transaction save(Transaction transaction) {
        TransactionJpaEntity entity = new TransactionJpaEntity(
                transaction.id(),
                transaction.description(),
                transaction.amount(),
                transaction.category(),
                transaction.transactionDate()
        );

        TransactionJpaEntity savedEntity = springDataTransactionRepository.save(entity);
        return toDomain(savedEntity);
    }

    @Override
    public List<Transaction> findAll() {
        return springDataTransactionRepository.findAll()
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private Transaction toDomain(TransactionJpaEntity entity) {
        return new Transaction(
                entity.getId(),
                entity.getDescription(),
                entity.getAmount(),
                entity.getCategory(),
                entity.getTransactionDate()
        );
    }
}
