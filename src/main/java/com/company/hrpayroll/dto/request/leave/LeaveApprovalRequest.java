// dto/request/leave/LeaveApprovalRequest.java
package com.company.hrpayroll.dto.request.leave;

public record LeaveApprovalRequest(
        boolean approved,
        String rejectionReason
) {}