// service/attendance/AttendanceService.java
package com.company.hrpayroll.service.attendance;

import com.company.hrpayroll.dto.request.attendance.AttendanceRequest;
import com.company.hrpayroll.dto.response.attendance.AttendanceResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface AttendanceService {
    AttendanceResponse clockInManually(AttendanceRequest request);
    AttendanceResponse clockOutManually(Long attendanceId, LocalDateTime clockOutTime);
    List<AttendanceResponse> getAttendanceForEmployee(Long employeeId, LocalDate start, LocalDate end);
    List<AttendanceResponse> getAttendanceForBranch(Long branchId, LocalDate start, LocalDate end);
}