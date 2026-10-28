// repository/loan/LoanRepository.java
package com.company.hrpayroll.repository.loan;

import com.company.hrpayroll.entity.loan.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {

    List<Loan> findByEmployeeId(Long employeeId);

    // Every loan of a set of employees — an agent's view of the loans of the staff they supply.
    List<Loan> findByEmployeeIdIn(List<Long> employeeIds);

    List<Loan> findByStatus(String status);

    List<Loan> findByEmployeeIdAndStatus(Long employeeId, String status);

    // Active loans for a set of employees — pulled during a payroll run.
    List<Loan> findByEmployeeIdInAndStatus(List<Long> employeeIds, String status);
}