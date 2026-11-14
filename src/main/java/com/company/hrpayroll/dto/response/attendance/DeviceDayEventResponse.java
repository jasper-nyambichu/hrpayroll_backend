// dto/response/attendance/DeviceDayEventResponse.java
package com.company.hrpayroll.dto.response.attendance;

import java.time.Instant;

/**
 * One step of one employee's day at a branch, as the branch PCs need it to decide what a scan means
 * (check in, or offer lunch / temporary check-out / check-out, or "back"). Every PC of the branch reads the
 * same list, so a check-in made on one PC is seen by the other.
 *
 * {@code eventId} is the id the PC itself gave the scan when it came from a PC, so a PC recognises its own
 * scans and never counts them twice; steps made elsewhere get a stable server id ("manual-12", "srv-out-12").
 */
public record DeviceDayEventResponse(
        Long employeeId,
        String terminalUserId,
        String eventId,
        String eventType,          // CLOCK_IN, LUNCH_OUT, EMERGENCY_OUT, BREAK_RETURN, CLOCK_OUT
        Instant at,
        Instant expectedBackAt,    // LUNCH_OUT / EMERGENCY_OUT only
        String reason              // EMERGENCY_OUT only
) {}
