// repository/attendance/AttendanceCheckoutRepository.java
package com.company.hrpayroll.repository.attendance;

import com.company.hrpayroll.entity.attendance.AttendanceCheckout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceCheckoutRepository extends JpaRepository<AttendanceCheckout, Long> {

    List<AttendanceCheckout> findByAttendanceIdInOrderByCheckedOutAtAsc(Collection<Long> attendanceIds);

    Optional<AttendanceCheckout> findByEventId(String eventId);

    Optional<AttendanceCheckout> findByReturnEventId(String returnEventId);

    Optional<AttendanceCheckout> findFirstByAttendanceIdAndCheckedInAtIsNullOrderByCheckedOutAtDesc(Long attendanceId);
}
