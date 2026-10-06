package com.company.hrpayroll.dto.response.attendance;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Response shapes for the ADMIN / SUPER_ADMIN / branch attendance dashboards. */
public final class AttendanceDashboardDtos {

    private AttendanceDashboardDtos() {}

    public record DashboardSummary(
            LocalDate date,
            LocalDateTime generatedAt,
            int expectedEmployees,
            int reported,
            int currentlyIn,
            int clockedOut,
            int notYetIn,
            int attendanceRatePercent,
            int devicesTotal,
            int devicesOnline,
            List<BranchSummary> branches
    ) {}

    public record BranchSummary(
            Long branchId,
            String branchName,
            String branchCode,
            int expected,
            int reported,
            int currentlyIn,
            int clockedOut,
            int notYetIn,
            int attendanceRatePercent,
            LocalDateTime lastActivityAt,
            String deviceStatus,            // ONLINE | OFFLINE | NO_DEVICE
            LocalDateTime deviceLastSyncAt
    ) {}

    public record ActivityItem(
            Long attendanceId,
            Long employeeId,
            String employeeName,
            String employeeNumber,
            Long branchId,
            String branchName,
            String type,                    // CLOCK_IN | CLOCK_OUT
            LocalDateTime occurredAt,
            String source
    ) {}

    public record RecordItem(
            Long id,
            Long employeeId,
            String employeeName,
            String employeeNumber,
            String position,
            Long branchId,
            String branchName,
            LocalDate attendanceDate,
            LocalDateTime clockIn,
            LocalDateTime clockOut,
            Double hoursWorked,
            String status,                  // IN_PROGRESS | COMPLETED
            String source
    ) {}

    public record DeviceItem(
            Long id,
            Long branchId,
            String branchName,
            String deviceSerial,
            String status,
            LocalDateTime lastSyncAt,
            boolean online
    ) {}

    public record SyncFailureItem(
            Long id,
            LocalDateTime createdAt,
            String eventId,
            String deviceSerial,
            Long employeeId,
            String employeeName,
            String reason
    ) {}
}