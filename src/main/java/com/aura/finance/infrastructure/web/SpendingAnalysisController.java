package com.aura.finance.infrastructure.web;

import com.aura.finance.application.port.in.AnalyzeSpendingUseCase;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/spending-analysis")
public class SpendingAnalysisController {

    private final AnalyzeSpendingUseCase analyzeSpendingUseCase;
    private final AnonymousSessionManager anonymousSessionManager;
    private final RequestRateLimiter requestRateLimiter;
    private final int analysisRequests;
    private final int analysisWindowMinutes;

    public SpendingAnalysisController(
            AnalyzeSpendingUseCase analyzeSpendingUseCase,
            AnonymousSessionManager anonymousSessionManager,
            RequestRateLimiter requestRateLimiter,
            @org.springframework.beans.factory.annotation.Value("${aura.rate-limit.analysis.requests}") int analysisRequests,
            @org.springframework.beans.factory.annotation.Value("${aura.rate-limit.analysis.window-minutes}") int analysisWindowMinutes
    ) {
        this.analyzeSpendingUseCase = analyzeSpendingUseCase;
        this.anonymousSessionManager = anonymousSessionManager;
        this.requestRateLimiter = requestRateLimiter;
        this.analysisRequests = analysisRequests;
        this.analysisWindowMinutes = analysisWindowMinutes;
    }

    @PostMapping
    public SpendingAnalysisResponse analyzeSpending(
            @Valid @RequestBody SpendingAnalysisRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        java.util.UUID sessionId = anonymousSessionManager.resolveSessionId(httpRequest, httpResponse);
        requestRateLimiter.assertAllowed(
                "spending-analysis",
                sessionId,
                httpRequest,
                analysisRequests,
                Duration.ofMinutes(analysisWindowMinutes)
        );
        AnalyzeSpendingUseCase.SpendingAnalysisResult result = analyzeSpendingUseCase.analyzeSpending(
                new AnalyzeSpendingUseCase.AnalyzeSpendingCommand(
                        sessionId,
                        request.startDate(),
                        request.endDate()
                )
        );

        return new SpendingAnalysisResponse(
                result.startDate(),
                result.endDate(),
                result.transactionCount(),
                result.totalSpent(),
                result.spendingByCategory(),
                result.summary(),
                result.insights(),
                result.recommendations()
        );
    }

    public record SpendingAnalysisRequest(
            @NotNull
            LocalDate startDate,
            @NotNull
            LocalDate endDate
    ) {
    }

    public record SpendingAnalysisResponse(
            LocalDate startDate,
            LocalDate endDate,
            int transactionCount,
            BigDecimal totalSpent,
            Map<String, BigDecimal> spendingByCategory,
            String summary,
            List<String> insights,
            List<String> recommendations
    ) {
    }
}
