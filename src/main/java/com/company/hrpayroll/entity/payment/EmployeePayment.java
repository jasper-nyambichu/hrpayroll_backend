// entity/payment/EmployeePayment.java
package com.company.hrpayroll.entity.payment;

import com.company.hrpayroll.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

// One payment an agent made to one employee by M-Pesa, recorded against the released payment request
// that funded it. The M-Pesa transaction code is unique, so the same payment can never be recorded twice.
// (Recording is the agent's own account of the payment; a live M-Pesa integration is a later step.)
@Entity
@Table(name = "employee_payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeePayment extends BaseEntity {

    @Column(nullable = false)
    private Long paymentRequestId;

    @Column(nullable = false)
    private Long agentId;

    @Column(nullable = false)
    private Long employeeId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, unique = true, length = 20)
    private String mpesaCode;

    @Column(nullable = false)
    private LocalDate paidOn;

    @Column(length = 255)
    private String note;

    @Column(nullable = false)
    private Long recordedByUserId;
}
