// entity/settlement/EmployeePayslip.java
package com.company.hrpayroll.entity.settlement;

import com.company.hrpayroll.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

// What one employee is due from one agent payment: pay + PAYE + NSSF + NITA - loans - damages.
@Entity
@Table(name = "employee_payslips")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeePayslip extends BaseEntity {

    @Column(nullable = false)
    private Long settlementId;

    @Column(nullable = false)
    private Long employeeId;

    @Column(nullable = false)
    private Integer daysWorked;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal dailyRate;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal pay;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal paye;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal nssf;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal nita;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal loan;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal damage;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amountDue;

    // The employee's monthly salary when the run was made (null on runs paid by the daily rate).
    @Column(precision = 15, scale = 2)
    private BigDecimal monthlySalary;

    // Off days in the period without a clock-in: paid like days worked.
    @Builder.Default
    @Column(nullable = false)
    private Integer offDaysPaid = 0;
}
