package com.aura.finance.infrastructure.ai;

import java.time.Duration;
import java.util.Optional;

public class NoOpAiResponseCache implements AiResponseCache {

    @Override
    public Optional<String> get(String namespace, String key) {
        return Optional.empty();
    }

    @Override
    public void put(String namespace, String key, String value, Duration ttl) {
    }
}
