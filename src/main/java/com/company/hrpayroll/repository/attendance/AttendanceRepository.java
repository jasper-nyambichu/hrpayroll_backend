// repository/attendance/AttendanceRepository.java
package com.company.hrpayroll.repository.attendance;

import com.company.hrpayroll.entity.attendance.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    // Idempotency check: the biometric sync uses this before saving.
    boolean existsByEventId(String eventId);

    Optional<Attendance> findByEventId(String eventId);

    List<Attendance> findByEmployeeIdAndAttendanceDateBetween(
            Long employeeId, LocalDate start, LocalDate end);

    List<Attendance> findByBranchIdAndAttendanceDateBetween(
            Long branchId, LocalDate start, LocalDate end);

    // Used to reject a second clock-in before a clock-out exists.
    Optional<Attendance> findByEmployeeIdAndAttendanceDateAndClockOutIsNull(
            Long employeeId, LocalDate attendanceDate);

    List<Attendance> findByEmployeeIdInAndAttendanceDateBetween(
            List<Long> employeeIds, LocalDate start, LocalDate end);
}