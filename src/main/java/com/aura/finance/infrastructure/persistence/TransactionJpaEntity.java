package com.aura.finance.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "transactions")
public class TransactionJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private LocalDate transactionDate;

    protected TransactionJpaEntity() {
    }

    public TransactionJpaEntity(
            UUID id,
            String description,
            BigDecimal amount,
            String category,
            LocalDate transactionDate
    ) {
        this.id = id;
        this.description = description;
        this.amount = amount;
        this.category = category;
        this.transactionDate = transactionDate;
    }

    public UUID getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCategory() {
        return category;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }
}
