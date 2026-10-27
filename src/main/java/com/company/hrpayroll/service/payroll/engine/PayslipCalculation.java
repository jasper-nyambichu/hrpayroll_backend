// service/payroll/engine/PayslipCalculation.java
package com.company.hrpayroll.service.payroll.engine;

import com.company.hrpayroll.entity.payroll.Payslip;
import com.company.hrpayroll.service.loan.LoanService;

import java.util.List;

/**
 * One employee's payslip together with what must be recorded once it is saved: the loan instalments
 * that were actually taken (a cap may have reduced them) and the damages that were deducted.
 */
public record PayslipCalculation(
        Payslip payslip,
        List<LoanService.LoanDeductionResult> loanDeductions,
        List<Long> damageIds
) {}