package com.aura.finance.infrastructure.ai;

import org.springframework.data.redis.core.StringRedisTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Optional;

public class RedisAiResponseCache implements AiResponseCache {

    private final StringRedisTemplate redisTemplate;
    private final String keyPrefix;

    public RedisAiResponseCache(StringRedisTemplate redisTemplate, String keyPrefix) {
        this.redisTemplate = redisTemplate;
        this.keyPrefix = keyPrefix;
    }

    @Override
    public Optional<String> get(String namespace, String key) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(buildCacheKey(namespace, key)));
    }

    @Override
    public void put(String namespace, String key, String value, Duration ttl) {
        redisTemplate.opsForValue().set(buildCacheKey(namespace, key), value, ttl);
    }

    private String buildCacheKey(String namespace, String key) {
        return keyPrefix + ":ai-cache:" + namespace + ":" + sha256(key);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is not available", exception);
        }
    }
}
