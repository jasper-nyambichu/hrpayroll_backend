// dto/response/leave/LeaveTypeResponse.java
package com.company.hrpayroll.dto.response.leave;

public record LeaveTypeResponse(
        Long id,
        String name,
        Integer defaultEntitlementDays,
        boolean paid
) {}