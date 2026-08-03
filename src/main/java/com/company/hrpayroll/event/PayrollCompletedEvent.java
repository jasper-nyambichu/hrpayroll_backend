// event/PayrollCompletedEvent.java
package com.company.hrpayroll.event;

public record PayrollCompletedEvent(Long payrollRunId, java.util.List<Long> employeeUserIds) {}