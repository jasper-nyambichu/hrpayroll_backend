// mapper/leave/LeaveTypeMapper.java
package com.company.hrpayroll.mapper.leave;

import com.company.hrpayroll.dto.response.leave.LeaveTypeResponse;
import com.company.hrpayroll.entity.leave.LeaveType;
import org.springframework.stereotype.Service;

@Service
public class LeaveTypeMapper {

    public LeaveTypeResponse toLeaveTypeResponse(LeaveType leaveType) {
        return new LeaveTypeResponse(
                leaveType.getId(),
                leaveType.getName(),
                leaveType.getDefaultEntitlementDays(),
                leaveType.isPaid()
        );
    }
}