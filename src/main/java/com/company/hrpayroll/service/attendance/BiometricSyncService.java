// service/attendance/BiometricSyncService.java
package com.company.hrpayroll.service.attendance;

import com.company.hrpayroll.dto.request.attendance.BiometricSyncRequest;
import com.company.hrpayroll.dto.response.attendance.AttendanceResponse;

public interface BiometricSyncService {
    AttendanceResponse syncEvent(BiometricSyncRequest request);
}