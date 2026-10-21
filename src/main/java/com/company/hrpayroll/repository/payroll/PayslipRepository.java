// repository/payroll/PayslipRepository.java
package com.company.hrpayroll.repository.payroll;

import com.company.hrpayroll.entity.payroll.Payslip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PayslipRepository extends JpaRepository<Payslip, Long> {

    List<Payslip> findByPayrollRunId(Long payrollRunId);

    List<Payslip> findByEmployeeId(Long employeeId);

    Optional<Payslip> findByPayrollRunIdAndEmployeeId(Long payrollRunId, Long employeeId);

    // Employees from the given set who already have a payslip in a COMPLETED run
    // whose period overlaps [periodStart, periodEnd] — the duplicate-payment guard.
    @Query("select distinct p.employeeId from Payslip p, PayrollRun r " +
            "where p.payrollRunId = r.id " +
            "and p.employeeId in :employeeIds " +
            "and r.status = com.company.hrpayroll.entity.payroll.PayrollRunStatus.COMPLETED " +
            "and r.periodStart <= :periodEnd and r.periodEnd >= :periodStart")
    List<Long> findEmployeeIdsPaidForOverlappingPeriod(
            @Param("employeeIds") Collection<Long> employeeIds,
            @Param("periodStart") LocalDate periodStart,
            @Param("periodEnd") LocalDate periodEnd);

    // Per run, the totals for DIRECT employees only (those with no agent) — the people the
    // company itself pays and remits for. Columns: runId, count, net, PAYE, NSSF, SHIF,
    // housing levy, NITA, loan deductions.
    @Query("select p.payrollRunId, count(p), sum(p.netPay), sum(p.payeDeduction), sum(p.nssfDeduction), " +
            "sum(p.shifDeduction), sum(p.housingLevyDeduction), sum(p.nitaDeduction), sum(p.loanDeduction) " +
            "from Payslip p, Employee e where p.employeeId = e.id and e.agentId is null " +
            "group by p.payrollRunId")
    List<Object[]> sumDirectEmployeesByRun();
}