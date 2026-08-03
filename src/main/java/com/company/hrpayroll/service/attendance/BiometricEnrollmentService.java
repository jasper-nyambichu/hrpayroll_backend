// service/attendance/BiometricEnrollmentService.java
package com.company.hrpayroll.service.attendance;

import com.company.hrpayroll.dto.request.attendance.EnrollmentRequest;
import com.company.hrpayroll.dto.response.attendance.BiometricTemplateResponse;
import com.company.hrpayroll.dto.response.attendance.TemplateSyncItemResponse;

import java.util.List;

public interface BiometricEnrollmentService {
    BiometricTemplateResponse enrollTemplate(EnrollmentRequest request);
    List<TemplateSyncItemResponse> getTemplatesForDevice(String deviceSerial);
}