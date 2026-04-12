package com.aura.finance.application.service;

import com.aura.finance.application.port.in.ExplainFinancialStrategyUseCase;
import com.aura.finance.application.port.out.FinancialStrategyExplainer;
import com.aura.finance.application.port.out.TransactionRepository;
import com.aura.finance.domain.model.Transaction;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class ExplainFinancialStrategyService implements ExplainFinancialStrategyUseCase {

    private static final MathContext MATH_CONTEXT = new MathContext(12, RoundingMode.HALF_UP);

    private final TransactionRepository transactionRepository;
    private final FinancialStrategyExplainer financialStrategyExplainer;

    public ExplainFinancialStrategyService(
            TransactionRepository transactionRepository,
            FinancialStrategyExplainer financialStrategyExplainer
    ) {
        this.transactionRepository = Objects.requireNonNull(transactionRepository, "transactionRepository must not be null");
        this.financialStrategyExplainer = Objects.requireNonNull(financialStrategyExplainer, "financialStrategyExplainer must not be null");
    }

    @Override
    public FinancialStrategyResult explainStrategy(FinancialStrategyCommand command) {
        List<Transaction> transactions = transactionRepository.findAllBySessionId(command.sessionId())
                .stream()
                .filter(transaction -> !transaction.transactionDate().isBefore(command.startDate()))
                .filter(transaction -> !transaction.transactionDate().isAfter(command.endDate()))
                .toList();

        BigDecimal totalSpent = transactions.stream()
                .map(Transaction::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        Map<String, BigDecimal> spendingByCategory = transactions.stream()
                .collect(Collectors.groupingBy(
                        Transaction::category,
                        Collectors.reducing(BigDecimal.ZERO, Transaction::amount, BigDecimal::add)
                ))
                .entrySet()
                .stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue(Comparator.reverseOrder()))
                .collect(LinkedHashMap::new, (map, entry) -> map.put(entry.getKey(), entry.getValue()), LinkedHashMap::putAll);

        Projection projection = project(
                command.plannedPurchaseAmount(),
                command.expectedMonthlyReturnRate(),
                command.timeHorizonMonths()
        );

        FinancialStrategyExplainer.FinancialStrategyExplanation explanation = financialStrategyExplainer.explain(
                new FinancialStrategyExplainer.FinancialStrategyRequest(
                        command.startDate(),
                        command.endDate(),
                        transactions.size(),
                        totalSpent,
                        spendingByCategory,
                        command.plannedPurchaseAmount(),
                        command.expectedMonthlyReturnRate(),
                        command.timeHorizonMonths(),
                        projection.futureValueIfInvested(),
                        projection.opportunityCost()
                )
        );

        return new FinancialStrategyResult(
                command.startDate(),
                command.endDate(),
                transactions.size(),
                totalSpent,
                spendingByCategory,
                command.plannedPurchaseAmount(),
                command.expectedMonthlyReturnRate(),
                command.timeHorizonMonths(),
                projection.futureValueIfInvested(),
                projection.opportunityCost(),
                explanation.spendingInsight(),
                explanation.cautionFlags(),
                explanation.recommendationText()
        );
    }

    private Projection project(BigDecimal purchaseAmount, BigDecimal monthlyReturnRate, Integer timeHorizonMonths) {
        if (purchaseAmount == null || monthlyReturnRate == null || timeHorizonMonths == null) {
            return new Projection(null, null);
        }

        if (purchaseAmount.signum() <= 0 || monthlyReturnRate.signum() < 0 || timeHorizonMonths <= 0) {
            throw new IllegalArgumentException("Invalid purchase simulation inputs");
        }

        BigDecimal growthFactor = BigDecimal.ONE.add(monthlyReturnRate, MATH_CONTEXT);
        BigDecimal compoundedGrowth = growthFactor.pow(timeHorizonMonths, MATH_CONTEXT);
        BigDecimal futureValueIfInvested = purchaseAmount.multiply(compoundedGrowth, MATH_CONTEXT)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal opportunityCost = futureValueIfInvested.subtract(purchaseAmount)
                .setScale(2, RoundingMode.HALF_UP);

        return new Projection(futureValueIfInvested, opportunityCost);
    }

    private record Projection(
            BigDecimal futureValueIfInvested,
            BigDecimal opportunityCost
    ) {
    }
}
