// service/attendance/AttendanceService.java
package com.company.hrpayroll.service.attendance;

import com.company.hrpayroll.dto.request.attendance.AttendanceRequest;
import com.company.hrpayroll.dto.response.attendance.AttendanceResponse;
import com.company.hrpayroll.dto.response.attendance.ManualCheckInResult;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface AttendanceService {
    AttendanceResponse clockInManually(AttendanceRequest request);

    /**
     * Checks the given employees in now, at the signed-in Branch Manager's own branch, for when the
     * fingerprint reader fails. Each one is checked separately, so one refusal does not stop the rest.
     */
    List<ManualCheckInResult> checkInAtMyBranch(List<Long> employeeIds);

    AttendanceResponse clockOutManually(Long attendanceId, LocalDateTime clockOutTime);
    List<AttendanceResponse> getAttendanceForEmployee(Long employeeId, LocalDate start, LocalDate end);
    List<AttendanceResponse> getAttendanceForBranch(Long branchId, LocalDate start, LocalDate end);

    /** For the live/polling dashboard feed — records created after `since`. */
    List<AttendanceResponse> getLiveAttendanceForBranch(Long branchId, LocalDateTime since);
}