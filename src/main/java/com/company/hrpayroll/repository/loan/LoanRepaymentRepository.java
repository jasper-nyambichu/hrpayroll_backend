// repository/loan/LoanRepaymentRepository.java
package com.company.hrpayroll.repository.loan;

import com.company.hrpayroll.entity.loan.LoanRepayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoanRepaymentRepository extends JpaRepository<LoanRepayment, Long> {

    List<LoanRepayment> findByLoanId(Long loanId);

    List<LoanRepayment> findByPayrollRunId(Long payrollRunId);
    List<LoanRepayment> findByPayrollRunIdIn(List<Long> payrollRunIds);
}