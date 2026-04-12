package com.aura.finance.application.port.in;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public interface AnalyzeSpendingUseCase {

    SpendingAnalysisResult analyzeSpending(AnalyzeSpendingCommand command);

    record AnalyzeSpendingCommand(
            java.util.UUID sessionId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        public AnalyzeSpendingCommand {
            Objects.requireNonNull(sessionId, "sessionId must not be null");
            Objects.requireNonNull(startDate, "startDate must not be null");
            Objects.requireNonNull(endDate, "endDate must not be null");

            if (endDate.isBefore(startDate)) {
                throw new IllegalArgumentException("endDate must not be before startDate");
            }
        }
    }

    record SpendingAnalysisResult(
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
