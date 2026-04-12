package com.aura.finance.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataTransactionRepository extends JpaRepository<TransactionJpaEntity, UUID> {

    java.util.List<TransactionJpaEntity> findAllBySessionId(UUID sessionId);

    java.util.Optional<TransactionJpaEntity> findByIdAndSessionId(UUID id, UUID sessionId);

    void deleteAllBySessionIdIn(java.util.Collection<UUID> sessionIds);
}
