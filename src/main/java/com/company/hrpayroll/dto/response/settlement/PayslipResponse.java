// dto/response/settlement/PayslipResponse.java
package com.company.hrpayroll.dto.response.settlement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

// amountDue is what the formula says the employee gets. amountRecorded is what the agent has recorded
// paying by M-Pesa against this period, and amountNotRecorded is the difference to chase.
public record PayslipResponse(
        Long id,
        Long settlementId,
        LocalDate periodStart,
        LocalDate periodEnd,
        Long employeeId,
        String employeeNumber,
        String employeeName,
        String branchName,
        String agentName,
        Integer daysWorked,
        BigDecimal dailyRate,
        BigDecimal pay,
        BigDecimal paye,
        BigDecimal nssf,
        BigDecimal nita,
        BigDecimal loan,
        BigDecimal damage,
        BigDecimal amountDue,
        BigDecimal amountRecorded,
        BigDecimal amountNotRecorded,
        List<RecordedPayment> payments
) {
    public record RecordedPayment(String mpesaCode, BigDecimal amount, LocalDate paidOn) {}
}
