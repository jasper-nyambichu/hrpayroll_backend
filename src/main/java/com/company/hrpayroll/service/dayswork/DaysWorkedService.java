// service/dayswork/DaysWorkedService.java
package com.company.hrpayroll.service.dayswork;

import com.company.hrpayroll.dto.request.dayswork.DaysWorkedLockRequest;
import com.company.hrpayroll.dto.response.dayswork.DaysWorkedResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface DaysWorkedService {

    // Every active employee of the branch for the period: the locked figure when the period has
    // been locked, otherwise the live count from attendance.
    List<DaysWorkedResponse> getForPeriod(Long branchId, LocalDate periodStart, LocalDate periodEnd);

    // Freezes the period for the branch so payroll can read it. Adjustments are optional.
    List<DaysWorkedResponse> lockPeriod(DaysWorkedLockRequest request, Long lockedByUserId);

    // Admin only: reopens a locked period so it can be corrected and locked again.
    void unlockPeriod(Long branchId, LocalDate periodStart, LocalDate periodEnd);

    // Called by payroll/engine during a run — not exposed via a controller. Only employees whose
    // period is locked appear in the map; payroll must not pay anyone missing from it.
    Map<Long, Integer> getLockedDays(List<Long> employeeIds, LocalDate periodStart, LocalDate periodEnd);
}
