// mapper/attendance/AttendanceMapper.java
package com.company.hrpayroll.mapper.attendance;

import com.company.hrpayroll.dto.response.attendance.AttendanceResponse;
import com.company.hrpayroll.entity.attendance.Attendance;
import org.springframework.stereotype.Service;

@Service
public class AttendanceMapper {

    public AttendanceResponse toAttendanceResponse(Attendance attendance, String employeeName) {
        return new AttendanceResponse(
                attendance.getId(),
                attendance.getEmployeeId(),
                employeeName,
                attendance.getBranchId(),
                attendance.getAttendanceDate(),
                attendance.getClockIn(),
                attendance.getClockOut(),
                attendance.getSource().name(),
                attendance.getStatus()
        );
    }
}