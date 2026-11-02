// repository/attendance/AttendanceCheckoutRepository.java
package com.company.hrpayroll.repository.attendance;

import com.company.hrpayroll.entity.attendance.AttendanceCheckout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface AttendanceCheckoutRepository extends JpaRepository<AttendanceCheckout, Long> {

    List<AttendanceCheckout> findByAttendanceIdInOrderByCheckedOutAtAsc(Collection<Long> attendanceIds);
}
