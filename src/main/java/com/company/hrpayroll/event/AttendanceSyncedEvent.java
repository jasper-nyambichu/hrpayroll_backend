// event/AttendanceSyncedEvent.java
package com.company.hrpayroll.event;

public record AttendanceSyncedEvent(Long attendanceId, Long employeeId, Long branchId) {}