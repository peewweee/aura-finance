package com.aura.finance.application.port.in;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public interface ExplainFinancialStrategyUseCase {

    FinancialStrategyResult explainStrategy(FinancialStrategyCommand command);

    record FinancialStrategyCommand(
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal plannedPurchaseAmount,
            BigDecimal expectedMonthlyReturnRate,
            Integer timeHorizonMonths
    ) {
        public FinancialStrategyCommand {
            Objects.requireNonNull(startDate, "startDate must not be null");
            Objects.requireNonNull(endDate, "endDate must not be null");

            if (endDate.isBefore(startDate)) {
                throw new IllegalArgumentException("endDate must not be before startDate");
            }
        }
    }

    record FinancialStrategyResult(
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
