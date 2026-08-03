// service/leave/LeaveService.java
package com.company.hrpayroll.service.leave;

import com.company.hrpayroll.dto.request.leave.LeaveApprovalRequest;
import com.company.hrpayroll.dto.request.leave.LeaveRequestCreateDto;
import com.company.hrpayroll.dto.response.leave.LeaveBalanceResponse;
import com.company.hrpayroll.dto.response.leave.LeaveRequestResponse;
import com.company.hrpayroll.dto.response.leave.LeaveTypeResponse;

import java.util.List;

public interface LeaveService {
    List<LeaveTypeResponse> getAllLeaveTypes();

    LeaveRequestResponse applyForLeave(Long employeeId, LeaveRequestCreateDto request);
    LeaveRequestResponse approveByManager(Long leaveRequestId, LeaveApprovalRequest request);
    LeaveRequestResponse approveByHr(Long leaveRequestId, LeaveApprovalRequest request);

    List<LeaveRequestResponse> getLeaveHistoryForEmployee(Long employeeId);
    List<LeaveRequestResponse> getPendingManagerApprovals(Long branchId);
    List<LeaveRequestResponse> getPendingHrApprovals();
    List<LeaveBalanceResponse> getLeaveBalances(Long employeeId);
}