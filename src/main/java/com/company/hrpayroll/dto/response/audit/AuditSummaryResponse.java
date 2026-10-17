// dto/response/audit/AuditSummaryResponse.java
package com.company.hrpayroll.dto.response.audit;

import java.time.LocalDate;
import java.util.List;

// What happened over a period: totals, the login picture, and where the activity was.
public record AuditSummaryResponse(
        LocalDate from,
        LocalDate to,
        long totalEvents,
        long loginSuccess,
        long loginFailed,
        long payrollRuns,
        List<Count> byEntityType,
        List<Count> byAction,
        List<UserCount> topUsers,
        List<DayCount> daily
) {
    public record Count(String label, long count) {}

    public record UserCount(Long userId, String username, long count) {}

    public record DayCount(LocalDate date, long count) {}
}