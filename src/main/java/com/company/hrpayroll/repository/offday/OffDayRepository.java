// repository/offday/OffDayRepository.java
package com.company.hrpayroll.repository.offday;

import com.company.hrpayroll.entity.offday.OffDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface OffDayRepository extends JpaRepository<OffDay, Long> {

    // Any off day touching the range: it starts by the range's end and ends after the range's start.
    List<OffDay> findByStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateAsc(
            LocalDate rangeEnd, LocalDate rangeStart);

    List<OffDay> findByAgentIdAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateAsc(
            Long agentId, LocalDate rangeEnd, LocalDate rangeStart);

    boolean existsByEmployeeIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long employeeId, LocalDate rangeEnd, LocalDate rangeStart);
}
