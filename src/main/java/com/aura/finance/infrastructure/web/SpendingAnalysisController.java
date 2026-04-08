package com.aura.finance.infrastructure.web;

import com.aura.finance.application.port.in.AnalyzeSpendingUseCase;
import jakarta.validation.Valid;
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
@RequestMapping("/spending-analysis")
public class SpendingAnalysisController {

    private final AnalyzeSpendingUseCase analyzeSpendingUseCase;

    public SpendingAnalysisController(AnalyzeSpendingUseCase analyzeSpendingUseCase) {
        this.analyzeSpendingUseCase = analyzeSpendingUseCase;
    }

    @PostMapping
    public SpendingAnalysisResponse analyzeSpending(@Valid @RequestBody SpendingAnalysisRequest request) {
        AnalyzeSpendingUseCase.SpendingAnalysisResult result = analyzeSpendingUseCase.analyzeSpending(
                new AnalyzeSpendingUseCase.AnalyzeSpendingCommand(
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
