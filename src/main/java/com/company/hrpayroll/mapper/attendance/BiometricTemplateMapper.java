// New file: mapper/attendance/BiometricTemplateMapper.java
package com.company.hrpayroll.mapper.attendance;

import com.company.hrpayroll.dto.response.attendance.BiometricTemplateResponse;
import com.company.hrpayroll.dto.response.attendance.TemplateSyncItemResponse;
import com.company.hrpayroll.entity.attendance.BiometricTemplate;
import org.springframework.stereotype.Service;

import java.util.Base64;

@Service
public class BiometricTemplateMapper {

    public BiometricTemplateResponse toBiometricTemplateResponse(BiometricTemplate template) {
        return new BiometricTemplateResponse(
                template.getId(),
                template.getEmployeeId(),
                template.getFingerIndex(),
                template.getQualityScore(),
                template.getEnrolledAt()
        );
    }

    public TemplateSyncItemResponse toTemplateSyncItemResponse(BiometricTemplate template) {
        return new TemplateSyncItemResponse(
                template.getEmployeeId(),
                template.getFingerIndex(),
                Base64.getEncoder().encodeToString(template.getTemplateData())
        );
    }
}