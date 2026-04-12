package com.aura.finance.infrastructure.ai;

import com.aura.finance.application.port.out.FinancialStrategyExplainer;

public class UnavailableFinancialStrategyExplainer implements FinancialStrategyExplainer {

    private final String message;

    public UnavailableFinancialStrategyExplainer(String message) {
        this.message = message;
    }

    @Override
    public FinancialStrategyExplanation explain(FinancialStrategyRequest request) {
        throw new AiIntegrationException(message);
    }
}
