package com.aura.finance.application.service;

import com.aura.finance.application.port.in.SimulatePurchaseUseCase;
import com.aura.finance.domain.model.OpportunityCostProjection;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public class SimulatePurchaseService implements SimulatePurchaseUseCase {

    private static final MathContext MATH_CONTEXT = new MathContext(12, RoundingMode.HALF_UP);

    @Override
    public SimulationResult simulatePurchase(SimulationCommand command) {
        OpportunityCostProjection projection = project(command);

        String explanation = """
                If you invest %s instead of spending it now, and it grows at %s per month for %d months, it could become %s. \
                The extra money you give up by buying now is about %s.
                """.formatted(
                projection.purchaseAmount(),
                formatPercentage(projection.monthlyReturnRate()),
                projection.timeHorizonMonths(),
                projection.futureValueIfInvested(),
                projection.opportunityCost()
        ).replaceAll("\\s+", " ").trim();

        return new SimulationResult(
                projection.purchaseAmount(),
                projection.monthlyReturnRate(),
                projection.timeHorizonMonths(),
                projection.futureValueIfInvested(),
                projection.opportunityCost(),
                explanation
        );
    }

    private OpportunityCostProjection project(SimulationCommand command) {
        BigDecimal growthFactor = BigDecimal.ONE.add(command.expectedMonthlyReturnRate(), MATH_CONTEXT);
        BigDecimal compoundedGrowth = growthFactor.pow(command.timeHorizonMonths(), MATH_CONTEXT);
        BigDecimal futureValue = command.purchaseAmount()
                .multiply(compoundedGrowth, MATH_CONTEXT)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal opportunityCost = futureValue.subtract(command.purchaseAmount())
                .setScale(2, RoundingMode.HALF_UP);

        return new OpportunityCostProjection(
                command.purchaseAmount().setScale(2, RoundingMode.HALF_UP),
                command.expectedMonthlyReturnRate(),
                command.timeHorizonMonths(),
                futureValue,
                opportunityCost
        );
    }

    private String formatPercentage(BigDecimal monthlyReturnRate) {
        return monthlyReturnRate
                .multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP) + "%";
    }
}
