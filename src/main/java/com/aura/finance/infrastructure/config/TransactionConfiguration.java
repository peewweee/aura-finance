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
import com.aura.finance.infrastructure.ai.AiResponseCache;
import com.aura.finance.infrastructure.ai.GeminiFinancialStrategyExplainer;
import com.aura.finance.infrastructure.ai.GeminiResponsesClient;
import com.aura.finance.infrastructure.ai.GeminiSpendingAnalysisAdvisor;
import com.aura.finance.infrastructure.ai.GeminiTransactionExtractor;
import com.aura.finance.infrastructure.ai.NoOpAiResponseCache;
import com.aura.finance.infrastructure.ai.RedisAiResponseCache;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.aura.finance.infrastructure.persistence.JpaTransactionRepositoryAdapter;
import com.aura.finance.infrastructure.persistence.SpringDataTransactionRepository;
import com.aura.finance.infrastructure.web.NoOpRequestRateLimiter;
import com.aura.finance.infrastructure.web.RedisRequestRateLimiter;
import com.aura.finance.infrastructure.web.RequestRateLimiter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

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
    public GeminiResponsesClient geminiResponsesClient(
            ObjectMapper objectMapper,
            @Value("${aura.ai.gemini.base-url}") String baseUrl,
            @Value("${aura.ai.gemini.api-key}") String apiKey,
            @Value("${aura.ai.gemini.model-name}") String modelName
    ) {
        return new GeminiResponsesClient(baseUrl, apiKey, modelName, objectMapper);
    }

    @Bean
    public TransactionExtractor transactionExtractor(
            GeminiResponsesClient geminiResponsesClient,
            ObjectMapper objectMapper,
            AiResponseCache aiResponseCache,
            @Value("${aura.cache.extract-ttl-minutes}") long extractCacheTtlMinutes
    ) {
        return new GeminiTransactionExtractor(geminiResponsesClient, objectMapper, aiResponseCache, extractCacheTtlMinutes);
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
            GeminiResponsesClient geminiResponsesClient,
            ObjectMapper objectMapper,
            AiResponseCache aiResponseCache,
            @Value("${aura.cache.analysis-ttl-minutes}") long analysisCacheTtlMinutes
    ) {
        return new GeminiSpendingAnalysisAdvisor(geminiResponsesClient, objectMapper, aiResponseCache, analysisCacheTtlMinutes);
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
            GeminiResponsesClient geminiResponsesClient,
            ObjectMapper objectMapper,
            AiResponseCache aiResponseCache,
            @Value("${aura.cache.strategy-ttl-minutes}") long strategyCacheTtlMinutes
    ) {
        return new GeminiFinancialStrategyExplainer(geminiResponsesClient, objectMapper, aiResponseCache, strategyCacheTtlMinutes);
    }

    @Bean
    public ExplainFinancialStrategyUseCase explainFinancialStrategyUseCase(
            TransactionRepository transactionRepository,
            FinancialStrategyExplainer financialStrategyExplainer
    ) {
        return new ExplainFinancialStrategyService(transactionRepository, financialStrategyExplainer);
    }

    @Bean
    public AiResponseCache aiResponseCache(
            @Value("${aura.redis.enabled:false}") boolean redisEnabled,
            @Value("${aura.redis.key-prefix:aura}") String keyPrefix,
            org.springframework.beans.factory.ObjectProvider<StringRedisTemplate> stringRedisTemplateProvider
    ) {
        if (!redisEnabled) {
            return new NoOpAiResponseCache();
        }

        StringRedisTemplate redisTemplate = stringRedisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            return new NoOpAiResponseCache();
        }

        return new RedisAiResponseCache(redisTemplate, keyPrefix);
    }

    @Bean
    public RequestRateLimiter requestRateLimiter(
            @Value("${aura.redis.enabled:false}") boolean redisEnabled,
            @Value("${aura.redis.key-prefix:aura}") String keyPrefix,
            org.springframework.beans.factory.ObjectProvider<StringRedisTemplate> stringRedisTemplateProvider
    ) {
        if (!redisEnabled) {
            return new NoOpRequestRateLimiter();
        }

        StringRedisTemplate redisTemplate = stringRedisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            return new NoOpRequestRateLimiter();
        }

        return new RedisRequestRateLimiter(redisTemplate, keyPrefix);
    }
}
