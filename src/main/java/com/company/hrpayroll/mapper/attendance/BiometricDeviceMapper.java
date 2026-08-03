// mapper/attendance/BiometricDeviceMapper.java
package com.company.hrpayroll.mapper.attendance;

import com.company.hrpayroll.dto.response.attendance.BiometricDeviceResponse;
import com.company.hrpayroll.dto.response.attendance.DeviceRegistrationResponse;
import com.company.hrpayroll.entity.attendance.BiometricDevice;
import org.springframework.stereotype.Service;

@Service
public class BiometricDeviceMapper {

    public BiometricDeviceResponse toBiometricDeviceResponse(BiometricDevice device, String branchName) {
        return new BiometricDeviceResponse(
                device.getId(),
                device.getBranchId(),
                branchName,
                device.getDeviceSerial(),
                device.getLastSyncAt(),
                device.getStatus()
        );
    }

    public DeviceRegistrationResponse toDeviceRegistrationResponse(BiometricDevice device, String plaintextToken) {
        return new DeviceRegistrationResponse(device.getId(), device.getDeviceSerial(), plaintextToken);
    }
}