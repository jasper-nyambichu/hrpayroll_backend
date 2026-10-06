// repository/attendance/AttendanceRepository.java
package com.company.hrpayroll.repository.attendance;

import com.company.hrpayroll.entity.attendance.Attendance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    // ---- idempotency ----
    boolean existsByEventId(String eventId);
    Optional<Attendance> findByEventId(String eventId);
    Optional<Attendance> findByClockOutEventId(String clockOutEventId);

    // ---- existing queries (used by manual attendance, reports, payroll) ----
    List<Attendance> findByEmployeeIdAndAttendanceDateBetween(Long employeeId, LocalDate start, LocalDate end);
    List<Attendance> findByBranchIdAndAttendanceDateBetween(Long branchId, LocalDate start, LocalDate end);
    Optional<Attendance> findByEmployeeIdAndAttendanceDateAndClockOutIsNull(Long employeeId, LocalDate attendanceDate);
    List<Attendance> findByEmployeeIdInAndAttendanceDateBetween(List<Long> employeeIds, LocalDate start, LocalDate end);

    // ---- open sessions (safe even if more than one open row exists) ----
    Optional<Attendance> findFirstByEmployeeIdAndAttendanceDateAndClockOutIsNullOrderByClockInDesc(
            Long employeeId, LocalDate attendanceDate);
    Optional<Attendance> findFirstByEmployeeIdAndClockOutIsNullOrderByClockInDesc(Long employeeId);

    // ---- admin / super-admin dashboards ----
    List<Attendance> findByAttendanceDate(LocalDate date);

    Page<Attendance> findByAttendanceDateBetween(LocalDate start, LocalDate end, Pageable pageable);
    Page<Attendance> findByBranchIdAndAttendanceDateBetween(Long branchId, LocalDate start, LocalDate end, Pageable pageable);
    Page<Attendance> findByAttendanceDateBetweenAndClockOutIsNull(LocalDate start, LocalDate end, Pageable pageable);
    Page<Attendance> findByBranchIdAndAttendanceDateBetweenAndClockOutIsNull(
            Long branchId, LocalDate start, LocalDate end, Pageable pageable);

    @Query("select a from Attendance a where a.clockIn >= :since or a.clockOut >= :since")
    List<Attendance> findActivitySince(@Param("since") LocalDateTime since);
}