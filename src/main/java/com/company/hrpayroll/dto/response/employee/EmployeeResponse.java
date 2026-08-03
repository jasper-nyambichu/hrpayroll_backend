// dto/response/employee/EmployeeResponse.java
package com.company.hrpayroll.dto.response.employee;

import com.company.hrpayroll.entity.employee.EmploymentType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EmployeeResponse(
        Long id,
        String fullName,
        String employeeNumber,
        String passportPhotoUrl,
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
        Long branchId,
        String branchName,
        Long departmentId,
        String departmentName,
        Long agentId,
        String agentName,
        String position,
        EmploymentType employmentType,
        BigDecimal basicSalary,
        LocalDate dateHired,
        String employmentStatus,
        String bankName,
        String bankAccountNumber,
        String emergencyContactName,
        String emergencyContactPhone
) {}