// entity/employee/Employee.java
package com.company.hrpayroll.entity.employee;

import com.company.hrpayroll.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "employees")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employee extends BaseEntity {

    // Personal information
    @Column(nullable = false, length = 150)
    private String fullName;

    @Column(nullable = false, unique = true, length = 30)
    private String employeeNumber;

    private String passportPhotoUrl;

    @Column(unique = true, length = 30)
    private String nationalId;

    private LocalDate dateOfBirth;

    @Column(length = 10)
    private String gender;

    @Column(length = 20)
    private String maritalStatus;

    private String phone;
    private String email;
    private String address;

    // Government details
    @Column(length = 20)
    private String kraPin;

    @Column(length = 20)
    private String shifNumber;

    @Column(length = 20)
    private String nssfNumber;

    @Column(length = 20)
    private String nitaNumber;

    // Employment details
    @Column(nullable = false)
    private Long branchId;

    private Long departmentId;

    // Null for directly-employed staff; set only for outsourced employees.
    private Long agentId;

    private String position;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private EmploymentType employmentType;

    // For reference only: pay comes from days worked x the daily rate.
    @Column(precision = 15, scale = 2)
    private BigDecimal basicSalary;

    private LocalDate dateHired;

    @Builder.Default
    @Column(nullable = false, length = 20)
    private String employmentStatus = "ACTIVE"; // ACTIVE, INACTIVE, TERMINATED

    // Bank details
    private String bankName;
    private String bankAccountNumber;

    // Emergency contact
    private String emergencyContactName;
    private String emergencyContactPhone;
}