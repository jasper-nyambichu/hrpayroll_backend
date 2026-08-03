// dto/response/shift/ShiftAssignmentResponse.java
package com.company.hrpayroll.dto.response.shift;

public record ShiftAssignmentResponse(
        Long id,
        Long shiftId,
        String shiftName,
        Long employeeId,
        Long departmentId,
        Long branchId
) {}