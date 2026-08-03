// mapper/leave/LeaveRequestMapper.java
package com.company.hrpayroll.mapper.leave;

import com.company.hrpayroll.dto.request.leave.LeaveRequestCreateDto;
import com.company.hrpayroll.dto.response.leave.LeaveRequestResponse;
import com.company.hrpayroll.entity.leave.LeaveRequest;
import com.company.hrpayroll.entity.leave.LeaveStatus;
import org.springframework.stereotype.Service;

@Service
public class LeaveRequestMapper {

    public LeaveRequest toLeaveRequest(LeaveRequestCreateDto dto, Long employeeId) {
        return LeaveRequest.builder()
                .employeeId(employeeId)
                .leaveTypeId(dto.leaveTypeId())
                .startDate(dto.startDate())
                .endDate(dto.endDate())
                .reason(dto.reason())
                .status(LeaveStatus.PENDING_MANAGER)
                .build();
    }

    public LeaveRequestResponse toLeaveRequestResponse(
            LeaveRequest request, String employeeName, String leaveTypeName) {
        return new LeaveRequestResponse(
                request.getId(),
                request.getEmployeeId(),
                employeeName,
                request.getLeaveTypeId(),
                leaveTypeName,
                request.getStartDate(),
                request.getEndDate(),
                request.getReason(),
                request.getStatus().name(),
                request.getRejectionReason()
        );
    }
}