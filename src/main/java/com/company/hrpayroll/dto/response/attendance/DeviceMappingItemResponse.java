package com.company.hrpayroll.dto.response.attendance;

// Pulled by the kiosk on startup and every few minutes: terminalUserId -> employee, plus the
// employee's display details so the kiosk can greet people by name without a manager login.
public record DeviceMappingItemResponse(
        String terminalUserId,
        Long employeeId,
        String employeeName,
        String employeeNumber,
        String position,
        String branchName
) {}