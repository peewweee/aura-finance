package com.aura.finance.infrastructure.ai;

import com.aura.finance.application.port.out.SpendingAnalysisAdvisor;

public class UnavailableSpendingAnalysisAdvisor implements SpendingAnalysisAdvisor {

    private final String message;

    public UnavailableSpendingAnalysisAdvisor(String message) {
        this.message = message;
    }

    @Override
    public SpendingAnalysis advise(SpendingAnalysisRequest request) {
        throw new AiIntegrationException(message);
    }
}
