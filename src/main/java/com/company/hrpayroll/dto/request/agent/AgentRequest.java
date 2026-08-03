// dto/request/agent/AgentRequest.java
package com.company.hrpayroll.dto.request.agent;

import com.company.hrpayroll.entity.agent.ChargeType;
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
        BigDecimal chargeAmount,
        BigDecimal chargePercentage,
        boolean vatRegistered
) {}