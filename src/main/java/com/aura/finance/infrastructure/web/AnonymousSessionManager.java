package com.aura.finance.infrastructure.web;

import com.aura.finance.infrastructure.persistence.AnonymousSessionJpaEntity;
import com.aura.finance.infrastructure.persistence.SpringDataAnonymousSessionRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

@Component
public class AnonymousSessionManager {

    static final String SESSION_COOKIE_NAME = "aura_session";
    private final SpringDataAnonymousSessionRepository anonymousSessionRepository;
    private final Duration sessionCookieMaxAge;

    public AnonymousSessionManager(
            SpringDataAnonymousSessionRepository anonymousSessionRepository,
            @org.springframework.beans.factory.annotation.Value("${aura.session.retention-days}") long retentionDays
    ) {
        this.anonymousSessionRepository = anonymousSessionRepository;
        this.sessionCookieMaxAge = Duration.ofDays(retentionDays);
    }

    public UUID resolveSessionId(HttpServletRequest request, HttpServletResponse response) {
        Optional<UUID> existingSessionId = readSessionId(request);
        if (existingSessionId.isPresent()) {
            return touchSession(existingSessionId.get(), response, request.isSecure());
        }

        UUID sessionId = UUID.randomUUID();
        Instant now = Instant.now();
        anonymousSessionRepository.save(new AnonymousSessionJpaEntity(
                sessionId,
                now,
                now,
                now.plus(sessionCookieMaxAge)
        ));
        writeSessionCookie(response, sessionId, request.isSecure());
        return sessionId;
    }

    private UUID touchSession(UUID sessionId, HttpServletResponse response, boolean secure) {
        Instant now = Instant.now();
        AnonymousSessionJpaEntity entity = anonymousSessionRepository.findById(sessionId)
                .map(existing -> new AnonymousSessionJpaEntity(
                        existing.getId(),
                        existing.getCreatedAt(),
                        now,
                        now.plus(sessionCookieMaxAge)
                ))
                .orElseGet(() -> new AnonymousSessionJpaEntity(
                        sessionId,
                        now,
                        now,
                        now.plus(sessionCookieMaxAge)
                ));

        anonymousSessionRepository.save(entity);
        writeSessionCookie(response, sessionId, secure);
        return sessionId;
    }

    private void writeSessionCookie(HttpServletResponse response, UUID sessionId, boolean secure) {
        ResponseCookie cookie = ResponseCookie.from(SESSION_COOKIE_NAME, sessionId.toString())
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(sessionCookieMaxAge)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private Optional<UUID> readSessionId(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null || cookies.length == 0) {
            return Optional.empty();
        }

        return Arrays.stream(cookies)
                .filter(cookie -> SESSION_COOKIE_NAME.equals(cookie.getName()))
                .findFirst()
                .flatMap(cookie -> {
                    try {
                        return Optional.of(UUID.fromString(cookie.getValue()));
                    } catch (IllegalArgumentException exception) {
                        return Optional.empty();
                    }
                });
    }
}
