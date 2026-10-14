// dto/request/agent/AgentRequest.java
package com.company.hrpayroll.dto.request.agent;

import com.company.hrpayroll.entity.agent.ChargeType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AgentRequest(
        @NotBlank(message = "Agent name is required") String name,
        String contactPerson,
        String phone,
        String email,
        String address,
        String bankName,
        String bankAccountNumber,
        String kraPin,
        LocalDate contractStartDate,
        LocalDate contractEndDate,
        @NotNull(message = "Charge type is required") ChargeType chargeType,
        @DecimalMin(value = "0.00", message = "Charge amount cannot be negative") BigDecimal chargeAmount,
        @DecimalMin(value = "0.00", message = "Charge percentage cannot be negative")
        @DecimalMax(value = "100.00", message = "Charge percentage cannot exceed 100") BigDecimal chargePercentage,
        boolean vatRegistered
) {}