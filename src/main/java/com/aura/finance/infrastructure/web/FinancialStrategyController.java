package com.aura.finance.infrastructure.web;

import com.aura.finance.application.port.in.ExplainFinancialStrategyUseCase;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
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
@RequestMapping("/financial-strategy")
public class FinancialStrategyController {

    private final ExplainFinancialStrategyUseCase explainFinancialStrategyUseCase;
    private final AnonymousSessionManager anonymousSessionManager;
    private final RequestRateLimiter requestRateLimiter;
    private final int strategyRequests;
    private final int strategyWindowMinutes;

    public FinancialStrategyController(
            ExplainFinancialStrategyUseCase explainFinancialStrategyUseCase,
            AnonymousSessionManager anonymousSessionManager,
            RequestRateLimiter requestRateLimiter,
            @org.springframework.beans.factory.annotation.Value("${aura.rate-limit.strategy.requests}") int strategyRequests,
            @org.springframework.beans.factory.annotation.Value("${aura.rate-limit.strategy.window-minutes}") int strategyWindowMinutes
    ) {
        this.explainFinancialStrategyUseCase = explainFinancialStrategyUseCase;
        this.anonymousSessionManager = anonymousSessionManager;
        this.requestRateLimiter = requestRateLimiter;
        this.strategyRequests = strategyRequests;
        this.strategyWindowMinutes = strategyWindowMinutes;
    }

    @PostMapping("/explain")
    public FinancialStrategyResponse explainStrategy(
            @Valid @RequestBody FinancialStrategyRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        java.util.UUID sessionId = anonymousSessionManager.resolveSessionId(httpRequest, httpResponse);
        requestRateLimiter.assertAllowed(
                "financial-strategy",
                sessionId,
                httpRequest,
                strategyRequests,
                Duration.ofMinutes(strategyWindowMinutes)
        );
        ExplainFinancialStrategyUseCase.FinancialStrategyResult result = explainFinancialStrategyUseCase.explainStrategy(
                new ExplainFinancialStrategyUseCase.FinancialStrategyCommand(
                        sessionId,
                        request.startDate(),
                        request.endDate(),
                        request.plannedPurchaseAmount(),
                        request.expectedMonthlyReturnRate(),
                        request.timeHorizonMonths()
                )
        );

        return new FinancialStrategyResponse(
                result.startDate(),
                result.endDate(),
                result.transactionCount(),
                result.totalSpent(),
                result.spendingByCategory(),
                result.plannedPurchaseAmount(),
                result.expectedMonthlyReturnRate(),
                result.timeHorizonMonths(),
                result.futureValueIfInvested(),
                result.opportunityCost(),
                result.spendingInsight(),
                result.cautionFlags(),
                result.recommendationText()
        );
    }

    public record FinancialStrategyRequest(
            @NotNull
            LocalDate startDate,
            @NotNull
            LocalDate endDate,
            @DecimalMin(value = "0.01")
            BigDecimal plannedPurchaseAmount,
            @DecimalMin(value = "0.00")
            BigDecimal expectedMonthlyReturnRate,
            Integer timeHorizonMonths
    ) {
    }

    public record FinancialStrategyResponse(
            LocalDate startDate,
            LocalDate endDate,
            int transactionCount,
            BigDecimal totalSpent,
            Map<String, BigDecimal> spendingByCategory,
            BigDecimal plannedPurchaseAmount,
            BigDecimal expectedMonthlyReturnRate,
            Integer timeHorizonMonths,
            BigDecimal futureValueIfInvested,
            BigDecimal opportunityCost,
            String spendingInsight,
            List<String> cautionFlags,
            String recommendationText
    ) {
    }
}
