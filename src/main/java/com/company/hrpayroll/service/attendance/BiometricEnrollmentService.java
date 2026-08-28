package com.company.hrpayroll.service.attendance;

import com.company.hrpayroll.dto.request.attendance.EnrollmentRequest;
import com.company.hrpayroll.dto.response.attendance.DeviceMappingItemResponse;

import java.util.List;

public interface BiometricEnrollmentService {
    void enrollMapping(EnrollmentRequest request);
    List<DeviceMappingItemResponse> getMappingsForDevice(String deviceSerial);
}