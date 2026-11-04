package com.company.hrpayroll.service.attendance;

import com.company.hrpayroll.dto.request.attendance.EnrollmentRequest;
import com.company.hrpayroll.dto.request.attendance.FingerprintEnrollRequest;
import com.company.hrpayroll.dto.response.attendance.DeviceMappingItemResponse;
import com.company.hrpayroll.dto.response.attendance.FingerprintSyncResponse;

import java.util.List;

public interface BiometricEnrollmentService {
    void enrollMapping(EnrollmentRequest request);
    List<DeviceMappingItemResponse> getMappingsForDevice(String deviceSerial);
    void saveFingerprint(FingerprintEnrollRequest request);
    FingerprintSyncResponse getFingerprintsForDevice(String deviceSerial);
    long countFingerprints(Long employeeId);
}