package com.aura.finance.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface SpringDataAnonymousSessionRepository extends JpaRepository<AnonymousSessionJpaEntity, UUID> {

    List<AnonymousSessionJpaEntity> findAllByExpiresAtBefore(Instant cutoff);
}
