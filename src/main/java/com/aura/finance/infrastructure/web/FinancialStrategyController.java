package com.aura.finance.infrastructure.web;

import com.aura.finance.application.port.in.ExplainFinancialStrategyUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/financial-strategy")
public class FinancialStrategyController {

    private final ExplainFinancialStrategyUseCase explainFinancialStrategyUseCase;

    public FinancialStrategyController(ExplainFinancialStrategyUseCase explainFinancialStrategyUseCase) {
        this.explainFinancialStrategyUseCase = explainFinancialStrategyUseCase;
    }

    @PostMapping("/explain")
    public FinancialStrategyResponse explainStrategy(@Valid @RequestBody FinancialStrategyRequest request) {
        ExplainFinancialStrategyUseCase.FinancialStrategyResult result = explainFinancialStrategyUseCase.explainStrategy(
                new ExplainFinancialStrategyUseCase.FinancialStrategyCommand(
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
