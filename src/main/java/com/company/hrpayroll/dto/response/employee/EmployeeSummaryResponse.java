// dto/response/employee/EmployeeSummaryResponse.java
package com.company.hrpayroll.dto.response.employee;

public record EmployeeSummaryResponse(
        Long id,
        String fullName,
        String employeeNumber,
        String branchName,
        String position,
        String employmentStatus
) {}