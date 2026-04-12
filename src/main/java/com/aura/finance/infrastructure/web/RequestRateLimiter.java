package com.aura.finance.infrastructure.web;

import jakarta.servlet.http.HttpServletRequest;

import java.time.Duration;
import java.util.UUID;

public interface RequestRateLimiter {

    void assertAllowed(String bucket, UUID sessionId, HttpServletRequest request, int maxRequests, Duration window);
}
