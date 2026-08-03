// event/LeaveApprovedEvent.java
package com.company.hrpayroll.event;

public record LeaveApprovedEvent(Long leaveRequestId, Long employeeUserId) {}