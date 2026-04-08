package com.aura.finance.application.port.in;

import java.math.BigDecimal;
import java.util.Objects;

public interface SimulatePurchaseUseCase {

    SimulationResult simulatePurchase(SimulationCommand command);

    record SimulationCommand(
            BigDecimal purchaseAmount,
            BigDecimal expectedMonthlyReturnRate,
            int timeHorizonMonths
    ) {
        public SimulationCommand {
            Objects.requireNonNull(purchaseAmount, "purchaseAmount must not be null");
            Objects.requireNonNull(expectedMonthlyReturnRate, "expectedMonthlyReturnRate must not be null");

            if (purchaseAmount.signum() <= 0) {
                throw new IllegalArgumentException("purchaseAmount must be greater than zero");
            }

            if (expectedMonthlyReturnRate.signum() < 0) {
                throw new IllegalArgumentException("expectedMonthlyReturnRate must not be negative");
            }

            if (timeHorizonMonths <= 0) {
                throw new IllegalArgumentException("timeHorizonMonths must be greater than zero");
            }
        }
    }

    record SimulationResult(
            BigDecimal purchaseAmount,
            BigDecimal expectedMonthlyReturnRate,
            int timeHorizonMonths,
            BigDecimal futureValueIfInvested,
            BigDecimal opportunityCost,
            String explanation
    ) {
    }
}
