// repository/payroll/PayrollRunRepository.java
package com.company.hrpayroll.repository.payroll;

import com.company.hrpayroll.entity.payroll.PayrollRun;
import com.company.hrpayroll.entity.payroll.PayrollRunStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PayrollRunRepository extends JpaRepository<PayrollRun, Long> {

    List<PayrollRun> findByStatus(PayrollRunStatus status);

    List<PayrollRun> findByInitiatedByUserId(Long userId);

    // Newest period first — backs the payroll runs list.
    List<PayrollRun> findAllByOrderByPeriodEndDescIdDesc();
}