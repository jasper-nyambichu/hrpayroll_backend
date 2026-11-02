// dto/response/employee/EmployeeDirectoryResponse.java
package com.company.hrpayroll.dto.response.employee;

import java.math.BigDecimal;

// One row of a branch's employee list, for an Admin or for an agent's own employees.
public record EmployeeDirectoryResponse(
        Long id,
        String employeeNumber,
        String fullName,
        String phone,
        String departmentName,
        String agentName,
        BigDecimal basicSalary,
        String position,
        String employmentStatus
) {}
