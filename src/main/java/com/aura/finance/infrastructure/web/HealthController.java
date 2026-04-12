package com.aura.finance.infrastructure.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
public class HealthController {

    @GetMapping("/health")
    public HealthResponse health() {
        return new HealthResponse("ok", "aura-finance", Instant.now());
    }

    public record HealthResponse(
            String status,
            String service,
            Instant timestamp
    ) {
    }
}
