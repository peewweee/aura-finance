package com.aura.finance.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

public record OpportunityCostProjection(
        BigDecimal purchaseAmount,
        BigDecimal monthlyReturnRate,
        int timeHorizonMonths,
        BigDecimal futureValueIfInvested,
        BigDecimal opportunityCost
) {
    public OpportunityCostProjection {
        Objects.requireNonNull(purchaseAmount, "purchaseAmount must not be null");
        Objects.requireNonNull(monthlyReturnRate, "monthlyReturnRate must not be null");
        Objects.requireNonNull(futureValueIfInvested, "futureValueIfInvested must not be null");
        Objects.requireNonNull(opportunityCost, "opportunityCost must not be null");

        if (purchaseAmount.signum() <= 0) {
            throw new IllegalArgumentException("purchaseAmount must be greater than zero");
        }

        if (timeHorizonMonths <= 0) {
            throw new IllegalArgumentException("timeHorizonMonths must be greater than zero");
        }
    }
}
