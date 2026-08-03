// dto/request/employee/EmployeeRequest.java
package com.company.hrpayroll.dto.request.employee;

import com.company.hrpayroll.entity.employee.EmploymentType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EmployeeRequest(
        @NotBlank(message = "Full name is required") String fullName,
        String nationalId,
        LocalDate dateOfBirth,
        String gender,
        String maritalStatus,
        String phone,
        String email,
        String address,
        String kraPin,
        String shifNumber,
        String nssfNumber,
        String nitaNumber,
        @NotNull(message = "Branch is required") Long branchId,
        Long departmentId,
        Long agentId,
        String position,
        @NotNull(message = "Employment type is required") EmploymentType employmentType,
        @NotNull(message = "Basic salary is required")
        @DecimalMin(value = "0.0", message = "Basic salary cannot be negative") BigDecimal basicSalary,
        LocalDate dateHired,
        String bankName,
        String bankAccountNumber,
        String emergencyContactName,
        String emergencyContactPhone
) {}