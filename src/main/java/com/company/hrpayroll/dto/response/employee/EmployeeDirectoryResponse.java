// dto/response/employee/EmployeeDirectoryResponse.java
package com.company.hrpayroll.dto.response.employee;

import java.math.BigDecimal;

// One row of a branch's employee list, for an Admin, a Branch Manager or an agent's own employees.
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
) {

    public EmployeeDirectoryResponse withoutSalary() {
        return new EmployeeDirectoryResponse(
                id, employeeNumber, fullName, phone, departmentName, agentName, null, position, employmentStatus);
    }
}
