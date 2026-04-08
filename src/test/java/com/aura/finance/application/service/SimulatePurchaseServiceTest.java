package com.aura.finance.application.service;

import com.aura.finance.application.port.in.SimulatePurchaseUseCase;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimulatePurchaseServiceTest {

    @Test
    void shouldCalculateFutureValueAndOpportunityCost() {
        SimulatePurchaseService service = new SimulatePurchaseService();

        SimulatePurchaseUseCase.SimulationResult result = service.simulatePurchase(
                new SimulatePurchaseUseCase.SimulationCommand(
                        new BigDecimal("50000.00"),
                        new BigDecimal("0.01"),
                        36
                )
        );

        assertEquals(new BigDecimal("50000.00"), result.purchaseAmount());
        assertEquals(new BigDecimal("0.01"), result.expectedMonthlyReturnRate());
        assertEquals(36, result.timeHorizonMonths());
        assertEquals(new BigDecimal("71538.44"), result.futureValueIfInvested());
        assertEquals(new BigDecimal("21538.44"), result.opportunityCost());
        assertTrue(result.explanation().contains("50000.00"));
        assertTrue(result.explanation().contains("71538.44"));
    }
}
