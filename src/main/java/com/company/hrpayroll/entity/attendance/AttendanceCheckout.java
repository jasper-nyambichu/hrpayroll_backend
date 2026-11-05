// entity/attendance/AttendanceCheckout.java
package com.company.hrpayroll.entity.attendance;

import com.company.hrpayroll.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// An employee leaving during the working day and coming back, recorded by a fingerprint scan at the
// branch reader. A day's first clock-in and last clock-out stay on Attendance.
@Entity
@Table(name = "attendance_checkouts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceCheckout extends BaseEntity {

    public static final String LUNCH = "LUNCH";
    public static final String EMERGENCY = "EMERGENCY";

    @Column(nullable = false)
    private Long attendanceId;

    @Column(nullable = false, length = 30)
    private String checkType;

    @Column(nullable = false)
    private LocalDateTime checkedOutAt;

    // Lunch: the end of the allowance, and coming back after it counts as late.
    // Emergency: only the time they said they would return, which is never counted as late.
    private LocalDateTime expectedBackAt;

    private LocalDateTime checkedInAt;

    private String reason;

    // The scans that made this row, so a retry from a PC's offline queue is not recorded twice.
    @Column(length = 100)
    private String eventId;

    @Column(length = 100)
    private String returnEventId;
}
