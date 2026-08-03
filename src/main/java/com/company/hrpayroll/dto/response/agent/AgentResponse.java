// dto/response/agent/AgentResponse.java
package com.company.hrpayroll.dto.response.agent;

import com.company.hrpayroll.entity.agent.ChargeType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AgentResponse(
        Long id,
        String name,
        String contactPerson,
        String phone,
        String email,
        String address,
        String bankName,
        String bankAccountNumber,
        String kraPin,
        LocalDate contractStartDate,
        LocalDate contractEndDate,
        ChargeType chargeType,
        BigDecimal chargeAmount,
        BigDecimal chargePercentage,
        boolean vatRegistered,
        String status,
        Integer employeeCount
) {}