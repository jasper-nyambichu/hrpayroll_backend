// repository/dayswork/DaysWorkedRecordRepository.java
package com.company.hrpayroll.repository.dayswork;

import com.company.hrpayroll.entity.dayswork.DaysWorkedRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DaysWorkedRecordRepository extends JpaRepository<DaysWorkedRecord, Long> {

    // Locked periods that end after a date: what has not been paid yet.
    List<DaysWorkedRecord> findByEmployeeIdAndPeriodEndAfter(Long employeeId, LocalDate after);

    // The locked records for exactly this period (what the Back Office screen shows).
    List<DaysWorkedRecord> findByBranchIdAndPeriodStartAndPeriodEnd(Long branchId, LocalDate periodStart, LocalDate periodEnd);

    // What payroll reads: the locked figure for each employee in exactly the run's period.
    List<DaysWorkedRecord> findByEmployeeIdInAndPeriodStartAndPeriodEnd(
            List<Long> employeeIds, LocalDate periodStart, LocalDate periodEnd);

    // Any locked record whose period overlaps [start, end] — a day must never be counted twice.
    List<DaysWorkedRecord> findByEmployeeIdInAndPeriodStartLessThanEqualAndPeriodEndGreaterThanEqual(
            List<Long> employeeIds, LocalDate end, LocalDate start);
}
