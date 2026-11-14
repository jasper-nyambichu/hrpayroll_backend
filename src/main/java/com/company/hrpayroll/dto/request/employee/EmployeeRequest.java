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
        // No longer asked for when adding an employee; still accepted so older clients keep working.
        EmploymentType employmentType,
        // Optional and for reference only: pay comes from days worked x the daily rate.
        @DecimalMin(value = "0.0", message = "Basic salary cannot be negative") BigDecimal basicSalary,
        LocalDate dateHired,
        String bankName,
        String bankAccountNumber,
        String emergencyContactName,
        String emergencyContactPhone,
        // Optional: the version the editor started from (EmployeeResponse.version). When it is given and the
        // employee has changed since, the edit is refused with 409 rather than overwriting the other change.
        Long version
) {}