package com.aura.finance.infrastructure.config;

import com.aura.finance.application.port.in.ConfirmExtractedTransactionsUseCase;
import com.aura.finance.application.port.in.AnalyzeSpendingUseCase;
import com.aura.finance.application.port.in.CreateTransactionUseCase;
import com.aura.finance.application.port.in.ExplainFinancialStrategyUseCase;
import com.aura.finance.application.port.in.ExtractTransactionsUseCase;
import com.aura.finance.application.port.in.GetTransactionByIdUseCase;
import com.aura.finance.application.port.in.ListTransactionsUseCase;
import com.aura.finance.application.port.in.SimulatePurchaseUseCase;
import com.aura.finance.application.port.out.FinancialStrategyExplainer;
import com.aura.finance.application.port.out.SpendingAnalysisAdvisor;
import com.aura.finance.application.port.out.TransactionExtractor;
import com.aura.finance.application.port.out.TransactionRepository;
import com.aura.finance.application.service.AnalyzeSpendingService;
import com.aura.finance.application.service.ConfirmExtractedTransactionsService;
import com.aura.finance.application.service.CreateTransactionService;
import com.aura.finance.application.service.ExplainFinancialStrategyService;
import com.aura.finance.application.service.ExtractTransactionsService;
import com.aura.finance.application.service.GetTransactionByIdService;
import com.aura.finance.application.service.SimulatePurchaseService;
import com.aura.finance.application.service.TransactionQueryService;
import com.aura.finance.infrastructure.ai.OllamaFinancialStrategyExplainer;
import com.aura.finance.infrastructure.ai.OllamaSpendingAnalysisAdvisor;
import com.aura.finance.infrastructure.ai.OllamaTransactionExtractor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.aura.finance.infrastructure.persistence.JpaTransactionRepositoryAdapter;
import com.aura.finance.infrastructure.persistence.SpringDataTransactionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TransactionConfiguration {

    @Bean
    public TransactionRepository transactionRepository(SpringDataTransactionRepository springDataTransactionRepository) {
        return new JpaTransactionRepositoryAdapter(springDataTransactionRepository);
    }

    @Bean
    public CreateTransactionUseCase createTransactionUseCase(TransactionRepository transactionRepository) {
        return new CreateTransactionService(transactionRepository);
    }

    @Bean
    public ListTransactionsUseCase listTransactionsUseCase(TransactionRepository transactionRepository) {
        return new TransactionQueryService(transactionRepository);
    }

    @Bean
    public GetTransactionByIdUseCase getTransactionByIdUseCase(TransactionRepository transactionRepository) {
        return new GetTransactionByIdService(transactionRepository);
    }

    @Bean
    public TransactionExtractor transactionExtractor(
            ObjectMapper objectMapper,
            @Value("${aura.ai.ollama.base-url}") String baseUrl,
            @Value("${aura.ai.ollama.model-name}") String modelName
    ) {
        return new OllamaTransactionExtractor(baseUrl, modelName, objectMapper);
    }

    @Bean
    public ExtractTransactionsUseCase extractTransactionsUseCase(TransactionExtractor transactionExtractor) {
        return new ExtractTransactionsService(transactionExtractor);
    }

    @Bean
    public ConfirmExtractedTransactionsUseCase confirmExtractedTransactionsUseCase(
            TransactionRepository transactionRepository
    ) {
        return new ConfirmExtractedTransactionsService(transactionRepository);
    }

    @Bean
    public SpendingAnalysisAdvisor spendingAnalysisAdvisor(
            ObjectMapper objectMapper,
            @Value("${aura.ai.ollama.base-url}") String baseUrl,
            @Value("${aura.ai.ollama.model-name}") String modelName
    ) {
        return new OllamaSpendingAnalysisAdvisor(baseUrl, modelName, objectMapper);
    }

    @Bean
    public AnalyzeSpendingUseCase analyzeSpendingUseCase(
            TransactionRepository transactionRepository,
            SpendingAnalysisAdvisor spendingAnalysisAdvisor
    ) {
        return new AnalyzeSpendingService(transactionRepository, spendingAnalysisAdvisor);
    }

    @Bean
    public SimulatePurchaseUseCase simulatePurchaseUseCase() {
        return new SimulatePurchaseService();
    }

    @Bean
    public FinancialStrategyExplainer financialStrategyExplainer(
            ObjectMapper objectMapper,
            @Value("${aura.ai.ollama.base-url}") String baseUrl,
            @Value("${aura.ai.ollama.model-name}") String modelName
    ) {
        return new OllamaFinancialStrategyExplainer(baseUrl, modelName, objectMapper);
    }

    @Bean
    public ExplainFinancialStrategyUseCase explainFinancialStrategyUseCase(
            TransactionRepository transactionRepository,
            FinancialStrategyExplainer financialStrategyExplainer
    ) {
        return new ExplainFinancialStrategyService(transactionRepository, financialStrategyExplainer);
    }
}
