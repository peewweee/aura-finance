package com.aura.finance.application.service;

import com.aura.finance.application.port.in.AnalyzeSpendingUseCase;
import com.aura.finance.application.port.out.SpendingAnalysisAdvisor;
import com.aura.finance.application.port.out.TransactionRepository;
import com.aura.finance.domain.model.Transaction;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class AnalyzeSpendingService implements AnalyzeSpendingUseCase {

    private final TransactionRepository transactionRepository;
    private final SpendingAnalysisAdvisor spendingAnalysisAdvisor;

    public AnalyzeSpendingService(
            TransactionRepository transactionRepository,
            SpendingAnalysisAdvisor spendingAnalysisAdvisor
    ) {
        this.transactionRepository = Objects.requireNonNull(transactionRepository, "transactionRepository must not be null");
        this.spendingAnalysisAdvisor = Objects.requireNonNull(spendingAnalysisAdvisor, "spendingAnalysisAdvisor must not be null");
    }

    @Override
    public SpendingAnalysisResult analyzeSpending(AnalyzeSpendingCommand command) {
        List<Transaction> transactions = transactionRepository.findAll()
                .stream()
                .filter(transaction -> !transaction.transactionDate().isBefore(command.startDate()))
                .filter(transaction -> !transaction.transactionDate().isAfter(command.endDate()))
                .toList();

        BigDecimal totalSpent = transactions.stream()
                .map(Transaction::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> spendingByCategory = transactions.stream()
                .collect(Collectors.groupingBy(
                        Transaction::category,
                        Collectors.reducing(BigDecimal.ZERO, Transaction::amount, BigDecimal::add)
                ))
                .entrySet()
                .stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue(Comparator.reverseOrder()))
                .collect(LinkedHashMap::new, (map, entry) -> map.put(entry.getKey(), entry.getValue()), LinkedHashMap::putAll);

        SpendingAnalysisAdvisor.SpendingAnalysis analysis = spendingAnalysisAdvisor.advise(
                new SpendingAnalysisAdvisor.SpendingAnalysisRequest(
                        command.startDate(),
                        command.endDate(),
                        transactions.size(),
                        totalSpent,
                        spendingByCategory
                )
        );

        return new SpendingAnalysisResult(
                command.startDate(),
                command.endDate(),
                transactions.size(),
                totalSpent,
                spendingByCategory,
                analysis.summary(),
                analysis.insights(),
                analysis.recommendations()
        );
    }
}
