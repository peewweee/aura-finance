package com.aura.finance.infrastructure.persistence;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class AnonymousSessionRetentionCleanup {

    private final SpringDataAnonymousSessionRepository anonymousSessionRepository;
    private final SpringDataTransactionRepository transactionRepository;

    public AnonymousSessionRetentionCleanup(
            SpringDataAnonymousSessionRepository anonymousSessionRepository,
            SpringDataTransactionRepository transactionRepository
    ) {
        this.anonymousSessionRepository = anonymousSessionRepository;
        this.transactionRepository = transactionRepository;
    }

    @Scheduled(cron = "${aura.session.cleanup-cron}")
    @Transactional
    public void deleteExpiredAnonymousData() {
        Instant cutoff = Instant.now();
        List<UUID> expiredSessionIds = anonymousSessionRepository.findAllByExpiresAtBefore(cutoff)
                .stream()
                .map(AnonymousSessionJpaEntity::getId)
                .toList();

        if (expiredSessionIds.isEmpty()) {
            return;
        }

        transactionRepository.deleteAllBySessionIdIn(expiredSessionIds);
        anonymousSessionRepository.deleteAllByIdInBatch(expiredSessionIds);
    }
}
