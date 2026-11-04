// dto/response/employee/EmployeeCreationRequestResponse.java
package com.company.hrpayroll.dto.response.employee;

import com.company.hrpayroll.dto.request.employee.EmployeeRequest;
import com.company.hrpayroll.entity.employee.EmployeeRequestStatus;

import java.time.LocalDateTime;

public record EmployeeCreationRequestResponse(
        Long id,
        EmployeeRequestStatus status,
        Long requestedByUserId,
        String requestedByName,
        LocalDateTime requestedAt,
        Long reviewedByUserId,
        String reviewedByName,
        LocalDateTime reviewedAt,
        String rejectionReason,
        Long createdEmployeeId,
        Long branchId,
        String branchName,
        String departmentName,
        String agentName,
        EmployeeRequest details
) {}