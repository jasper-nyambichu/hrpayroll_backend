package com.company.hrpayroll.dto.response.attendance;

// Pulled by the kiosk on startup and every few minutes: terminalUserId -> employee, plus the
// employee's display details so the kiosk can greet people by name without a manager login.
public record DeviceMappingItemResponse(
        String terminalUserId,
        Long employeeId,
        String employeeName,
        String employeeNumber,
        String position,
        String branchName,
        // BRANCH_MANAGER or BACK_OFFICE when this employee is also the branch's manager or Back Office
        // (their login is linked to this employee record); null for everyone else.
        String staffRole
) {
    public DeviceMappingItemResponse withStaffRole(String role) {
        return new DeviceMappingItemResponse(terminalUserId, employeeId, employeeName, employeeNumber, position, branchName, role);
    }
}