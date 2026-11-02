package com.company.hrpayroll.service.attendance;

import com.company.hrpayroll.dto.response.PageResponse;
import com.company.hrpayroll.dto.response.attendance.AttendanceDashboardDtos.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface AttendanceDashboardService {
    DashboardSummary summary(LocalDate date, Long branchId);
    List<ActivityItem> activity(Long branchId, LocalDateTime since, int limit);
    PageResponse<RecordItem> records(Long branchId, LocalDate start, LocalDate end, boolean openOnly, int page, int size);
    List<DeviceItem> devices(Long branchId);
    List<SyncFailureItem> failures();
    AgentToday agentToday(Long agentId);
}