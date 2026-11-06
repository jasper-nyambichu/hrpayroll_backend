// service/attendance/BiometricDeviceService.java
package com.company.hrpayroll.service.attendance;

import com.company.hrpayroll.dto.request.attendance.DeviceRegistrationRequest;
import com.company.hrpayroll.dto.response.attendance.BiometricDeviceResponse;
import com.company.hrpayroll.dto.response.attendance.DeviceRegistrationResponse;

import java.util.List;

public interface BiometricDeviceService {
    DeviceRegistrationResponse registerDevice(DeviceRegistrationRequest request);
    List<BiometricDeviceResponse> getDevicesByBranch(Long branchId);
    void deactivateDevice(Long deviceId);
    void recordHeartbeat(String deviceSerial, com.company.hrpayroll.dto.request.attendance.DeviceHeartbeatRequest request);
}