// service/attendance/impl/AttendanceServiceImpl.java
package com.company.hrpayroll.service.attendance.impl;

import com.company.hrpayroll.dto.request.attendance.AttendanceRequest;
import com.company.hrpayroll.dto.response.attendance.AttendanceResponse;
import com.company.hrpayroll.entity.attendance.Attendance;
import com.company.hrpayroll.entity.attendance.AttendanceSource;
import com.company.hrpayroll.entity.employee.Employee;
import com.company.hrpayroll.exception.InvalidOperationException;
import com.company.hrpayroll.exception.ResourceNotFoundException;
import com.company.hrpayroll.mapper.attendance.AttendanceMapper;
import com.company.hrpayroll.repository.attendance.AttendanceRepository;
import com.company.hrpayroll.repository.employee.EmployeeRepository;
import com.company.hrpayroll.service.attendance.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final AttendanceMapper attendanceMapper;

    @Override
    public AttendanceResponse clockInManually(AttendanceRequest request) {
        Employee employee = employeeRepository.findById(request.employeeId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee not found with id: " + request.employeeId()));

        LocalDate date = request.clockIn().toLocalDate();

        boolean alreadyOpen = attendanceRepository
                .findByEmployeeIdAndAttendanceDateAndClockOutIsNull(request.employeeId(), date)
                .isPresent();
        if (alreadyOpen) {
            throw new InvalidOperationException("This employee already has an open clock-in for today");
        }

        Attendance attendance = Attendance.builder()
                .employeeId(employee.getId())
                .branchId(employee.getBranchId())
                .attendanceDate(date)
                .clockIn(request.clockIn())
                .clockOut(request.clockOut())
                .source(AttendanceSource.MANUAL)
                .status("PRESENT")
                .build();

        Attendance saved = attendanceRepository.save(attendance);
        return attendanceMapper.toAttendanceResponse(saved, employee.getFullName());
    }

    @Override
    public AttendanceResponse clockOutManually(Long attendanceId, LocalDateTime clockOutTime) {
        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance record not found with id: " + attendanceId));

        if (attendance.getClockOut() != null) {
            throw new InvalidOperationException("This attendance record is already clocked out");
        }

        attendance.setClockOut(clockOutTime);
        Attendance saved = attendanceRepository.save(attendance);

        String employeeName = employeeRepository.findById(saved.getEmployeeId())
                .map(Employee::getFullName).orElse(null);
        return attendanceMapper.toAttendanceResponse(saved, employeeName);
    }

    @Override
    public List<AttendanceResponse> getAttendanceForEmployee(Long employeeId, LocalDate start, LocalDate end) {
        String employeeName = employeeRepository.findById(employeeId)
                .map(Employee::getFullName).orElse(null);

        return attendanceRepository.findByEmployeeIdAndAttendanceDateBetween(employeeId, start, end).stream()
                .map(a -> attendanceMapper.toAttendanceResponse(a, employeeName))
                .toList();
    }

    @Override
    public List<AttendanceResponse> getAttendanceForBranch(Long branchId, LocalDate start, LocalDate end) {
        return attendanceRepository.findByBranchIdAndAttendanceDateBetween(branchId, start, end).stream()
                .map(a -> {
                    String employeeName = employeeRepository.findById(a.getEmployeeId())
                            .map(Employee::getFullName).orElse(null);
                    return attendanceMapper.toAttendanceResponse(a, employeeName);
                })
                .toList();
    }

    @Override
    public List<AttendanceResponse> getLiveAttendanceForBranch(Long branchId, LocalDateTime since) {
        return List.of();
    }
}