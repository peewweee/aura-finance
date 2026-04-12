package com.aura.finance.infrastructure.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.UUID;

public class RedisRequestRateLimiter implements RequestRateLimiter {

    private final StringRedisTemplate redisTemplate;
    private final String keyPrefix;

    public RedisRequestRateLimiter(StringRedisTemplate redisTemplate, String keyPrefix) {
        this.redisTemplate = redisTemplate;
        this.keyPrefix = keyPrefix;
    }

    @Override
    public void assertAllowed(String bucket, UUID sessionId, HttpServletRequest request, int maxRequests, Duration window) {
        String key = keyPrefix + ":rate-limit:" + bucket + ":" + sessionId + ":" + clientIdentifier(request);
        Long current = redisTemplate.opsForValue().increment(key);
        if (current == null) {
            return;
        }

        if (current == 1L) {
            redisTemplate.expire(key, window);
        }

        if (current > maxRequests) {
            throw new RateLimitExceededException("Too many requests. Please wait a bit before trying again.");
        }
    }

    private String clientIdentifier(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
