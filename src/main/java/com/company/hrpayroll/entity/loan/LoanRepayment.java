// entity/loan/LoanRepayment.java
package com.company.hrpayroll.entity.loan;

import com.company.hrpayroll.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "loan_repayments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanRepayment extends BaseEntity {

    @Column(nullable = false)
    private Long loanId;

    // Links this repayment to the payroll run that deducted it.
    @Column(nullable = false)
    private Long payrollRunId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amountPaid;
}