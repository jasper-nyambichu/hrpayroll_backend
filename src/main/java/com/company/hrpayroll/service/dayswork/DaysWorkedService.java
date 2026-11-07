// service/dayswork/DaysWorkedService.java
package com.company.hrpayroll.service.dayswork;

import com.company.hrpayroll.dto.response.dayswork.DaysWorkedResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface DaysWorkedService {

    // Every active employee of the branch for the period, with the days counted from attendance.
    List<DaysWorkedResponse> getForPeriod(Long branchId, LocalDate periodStart, LocalDate periodEnd);

    // Called by the agent payment calculation: days worked per employee for the period. Days are no
    // longer locked first, so this is the live attendance count; a figure locked before locking was
    // removed still stands for its exact period. Every requested employee is in the map (0 if none).
    Map<Long, Integer> getDaysForPayment(List<Long> employeeIds, LocalDate periodStart, LocalDate periodEnd);
}
