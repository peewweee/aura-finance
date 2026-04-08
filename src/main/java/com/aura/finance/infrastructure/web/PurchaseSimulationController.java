package com.aura.finance.infrastructure.web;

import com.aura.finance.application.port.in.SimulatePurchaseUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/purchase-simulation")
public class PurchaseSimulationController {

    private final SimulatePurchaseUseCase simulatePurchaseUseCase;

    public PurchaseSimulationController(SimulatePurchaseUseCase simulatePurchaseUseCase) {
        this.simulatePurchaseUseCase = simulatePurchaseUseCase;
    }

    @PostMapping
    public SimulationResponse simulatePurchase(@Valid @RequestBody SimulationRequest request) {
        SimulatePurchaseUseCase.SimulationResult result = simulatePurchaseUseCase.simulatePurchase(
                new SimulatePurchaseUseCase.SimulationCommand(
                        request.purchaseAmount(),
                        request.expectedMonthlyReturnRate(),
                        request.timeHorizonMonths()
                )
        );

        return new SimulationResponse(
                result.purchaseAmount(),
                result.expectedMonthlyReturnRate(),
                result.timeHorizonMonths(),
                result.futureValueIfInvested(),
                result.opportunityCost(),
                result.explanation()
        );
    }

    public record SimulationRequest(
            @NotNull
            @DecimalMin(value = "0.01")
            BigDecimal purchaseAmount,
            @NotNull
            @DecimalMin(value = "0.00")
            BigDecimal expectedMonthlyReturnRate,
            @NotNull
            Integer timeHorizonMonths
    ) {
    }

    public record SimulationResponse(
            BigDecimal purchaseAmount,
            BigDecimal expectedMonthlyReturnRate,
            int timeHorizonMonths,
            BigDecimal futureValueIfInvested,
            BigDecimal opportunityCost,
            String explanation
    ) {
    }
}
