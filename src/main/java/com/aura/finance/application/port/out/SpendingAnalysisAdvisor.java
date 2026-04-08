package com.aura.finance.application.port.out;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public interface SpendingAnalysisAdvisor {

    SpendingAnalysis advise(SpendingAnalysisRequest request);

    record SpendingAnalysisRequest(
            LocalDate startDate,
            LocalDate endDate,
            int transactionCount,
            BigDecimal totalSpent,
            Map<String, BigDecimal> spendingByCategory
    ) {
        public SpendingAnalysisRequest {
            Objects.requireNonNull(startDate, "startDate must not be null");
            Objects.requireNonNull(endDate, "endDate must not be null");
            Objects.requireNonNull(totalSpent, "totalSpent must not be null");
            Objects.requireNonNull(spendingByCategory, "spendingByCategory must not be null");
        }
    }

    record SpendingAnalysis(
            String summary,
            List<String> insights,
            List<String> recommendations
    ) {
        public SpendingAnalysis {
            Objects.requireNonNull(summary, "summary must not be null");
            Objects.requireNonNull(insights, "insights must not be null");
            Objects.requireNonNull(recommendations, "recommendations must not be null");
        }
    }
}
