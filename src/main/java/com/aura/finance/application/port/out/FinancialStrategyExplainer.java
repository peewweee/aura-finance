package com.aura.finance.application.port.out;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public interface FinancialStrategyExplainer {

    FinancialStrategyExplanation explain(FinancialStrategyRequest request);

    record FinancialStrategyRequest(
            LocalDate startDate,
            LocalDate endDate,
            int transactionCount,
            BigDecimal totalSpent,
            Map<String, BigDecimal> spendingByCategory,
            BigDecimal plannedPurchaseAmount,
            BigDecimal expectedMonthlyReturnRate,
            Integer timeHorizonMonths,
            BigDecimal futureValueIfInvested,
            BigDecimal opportunityCost
    ) {
        public FinancialStrategyRequest {
            Objects.requireNonNull(startDate, "startDate must not be null");
            Objects.requireNonNull(endDate, "endDate must not be null");
            Objects.requireNonNull(totalSpent, "totalSpent must not be null");
            Objects.requireNonNull(spendingByCategory, "spendingByCategory must not be null");
        }
    }

    record FinancialStrategyExplanation(
            String spendingInsight,
            List<String> cautionFlags,
            String recommendationText
    ) {
        public FinancialStrategyExplanation {
            Objects.requireNonNull(spendingInsight, "spendingInsight must not be null");
            Objects.requireNonNull(cautionFlags, "cautionFlags must not be null");
            Objects.requireNonNull(recommendationText, "recommendationText must not be null");
        }
    }
}
