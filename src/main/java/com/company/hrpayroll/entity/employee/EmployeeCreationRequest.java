// entity/employee/EmployeeCreationRequest.java
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
import java.time.LocalDateTime;

// A "new employee" waiting for Admin approval. Mirrors EmployeeRequest field
// for field, so approving it is just: build an EmployeeRequest -> createEmployee().
@Entity
@Table(name = "employee_creation_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeCreationRequest extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 20)
    private EmployeeRequestStatus status = EmployeeRequestStatus.PENDING;

    @Column(nullable = false)
    private Long requestedByUserId;

    private Long reviewedByUserId;
    private LocalDateTime reviewedAt;

    @Column(length = 500)
    private String rejectionReason;

    // Set once approved — points at the real Employee row.
    private Long createdEmployeeId;

    // ---- payload (same fields as EmployeeRequest) ----
    @Column(nullable = false, length = 150)
    private String fullName;

    @Column(length = 30)
    private String nationalId;

    private LocalDate dateOfBirth;

    @Column(length = 10)
    private String gender;

    @Column(length = 20)
    private String maritalStatus;

    private String phone;
    private String email;
    private String address;

    @Column(length = 20)
    private String kraPin;

    @Column(length = 20)
    private String shifNumber;

    @Column(length = 20)
    private String nssfNumber;

    @Column(length = 20)
    private String nitaNumber;

    @Column(nullable = false)
    private Long branchId;

    private Long departmentId;
    private Long agentId;
    private String position;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private EmploymentType employmentType;

    @Column(precision = 15, scale = 2)
    private BigDecimal basicSalary;

    private LocalDate dateHired;

    private String bankName;
    private String bankAccountNumber;
    private String emergencyContactName;
    private String emergencyContactPhone;
}