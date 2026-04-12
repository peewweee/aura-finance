package com.aura.finance.infrastructure.ai;

import java.time.Duration;
import java.util.Optional;

public interface AiResponseCache {

    Optional<String> get(String namespace, String key);

    void put(String namespace, String key, String value, Duration ttl);
}
