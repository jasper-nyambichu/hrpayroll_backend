// entity/attendance/Attendance.java
package com.company.hrpayroll.entity.attendance;

import com.company.hrpayroll.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendance")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attendance extends BaseEntity {

    @Column(nullable = false)
    private Long employeeId;

    @Column(nullable = false)
    private Long branchId;

    @Column(nullable = false)
    private LocalDate attendanceDate;

    private LocalDateTime clockIn;
    private LocalDateTime clockOut;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttendanceSource source;

    @Builder.Default
    @Column(nullable = false, length = 20)
    private String status = "PRESENT"; // PRESENT, LATE, ABSENT, OVERTIME, ON_LEAVE

    // Set only when source = BIOMETRIC. Used to reject duplicate syncs.
    @Column(unique = true, length = 100)
    private String eventId;

    private Long deviceId;
    @Column(length = 30)
    private String verifyMethod;
}