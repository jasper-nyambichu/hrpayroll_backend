// dto/response/settlement/AgentSettlementResponse.java
package com.company.hrpayroll.dto.response.settlement;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AgentSettlementResponse(
        Long id,
        Long agentId,
        String agentName,
        LocalDate periodStart,
        LocalDate periodEnd,
        Integer employeeCount,
        Integer daysWorkedTotal,
        BigDecimal daysPay,
        BigDecimal payeTotal,
        BigDecimal nssfTotal,
        BigDecimal nitaTotal,
        BigDecimal agentFee,
        BigDecimal vatRateUsed,
        BigDecimal vatAmount,
        BigDecimal loanTotal,
        BigDecimal damageTotal,
        BigDecimal advanceRecovered,
        BigDecimal amountPayable,
        Long paymentRequestId,
        String payBasis        // SALARY, or DAILY_RATE for runs made before salaries were used
) {}
