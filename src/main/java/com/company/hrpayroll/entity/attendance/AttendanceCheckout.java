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

// An employee leaving during the working day and coming back. The Back Office application records
// these; a day's first clock-in and last clock-out stay on Attendance.
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

    // Set for a break with a time limit. Coming back after it counts as late.
    private LocalDateTime expectedBackAt;

    private LocalDateTime checkedInAt;

    private String reason;
}
