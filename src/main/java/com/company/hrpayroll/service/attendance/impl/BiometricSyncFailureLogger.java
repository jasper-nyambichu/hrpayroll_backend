// service/attendance/impl/BiometricSyncFailureLogger.java
package com.company.hrpayroll.service.attendance.impl;

import com.company.hrpayroll.dto.request.attendance.BiometricSyncRequest;
import com.company.hrpayroll.entity.attendance.BiometricSyncFailure;
import com.company.hrpayroll.repository.attendance.BiometricSyncFailureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BiometricSyncFailureLogger {

    private final BiometricSyncFailureRepository syncFailureRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(BiometricSyncRequest request, Long employeeId, String reason) {
        BiometricSyncFailure failure = BiometricSyncFailure.builder()
                .eventId(request.eventId())
                .deviceSerial(request.deviceSerial())
                .employeeId(employeeId)
                .reason(reason)
                .build();
        syncFailureRepository.save(failure);
    }
}