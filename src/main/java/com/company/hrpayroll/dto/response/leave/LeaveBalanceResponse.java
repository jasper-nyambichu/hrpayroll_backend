// dto/response/leave/LeaveBalanceResponse.java
package com.company.hrpayroll.dto.response.leave;

public record LeaveBalanceResponse(
        Long leaveTypeId,
        String leaveTypeName,
        int entitledDays,
        int usedDays,
        int remainingDays
) {}