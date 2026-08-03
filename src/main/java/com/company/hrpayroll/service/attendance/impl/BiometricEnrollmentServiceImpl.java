// service/attendance/impl/BiometricEnrollmentServiceImpl.java
package com.company.hrpayroll.service.attendance.impl;

import com.company.hrpayroll.dto.request.attendance.EnrollmentRequest;
import com.company.hrpayroll.dto.response.attendance.BiometricTemplateResponse;
import com.company.hrpayroll.dto.response.attendance.TemplateSyncItemResponse;
import com.company.hrpayroll.entity.attendance.BiometricDevice;
import com.company.hrpayroll.entity.attendance.BiometricTemplate;
import com.company.hrpayroll.exception.InvalidOperationException;
import com.company.hrpayroll.exception.ResourceNotFoundException;
import com.company.hrpayroll.mapper.attendance.BiometricTemplateMapper;
import com.company.hrpayroll.repository.attendance.BiometricDeviceRepository;
import com.company.hrpayroll.repository.attendance.BiometricTemplateRepository;
import com.company.hrpayroll.repository.employee.EmployeeRepository;
import com.company.hrpayroll.service.attendance.BiometricEnrollmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BiometricEnrollmentServiceImpl implements BiometricEnrollmentService {

    // Enrollment below this quality score is rejected and the branch is
    // asked to re-scan — a poor-quality template causes false rejects for
    // months afterward, so this is enforced here, not left to judgement.
    private static final double MIN_QUALITY_SCORE = 60.0;

    // At most two fingers per employee (primary + backup).
    private static final int MAX_FINGERS_PER_EMPLOYEE = 2;

    private final BiometricTemplateRepository biometricTemplateRepository;
    private final BiometricDeviceRepository biometricDeviceRepository;
    private final EmployeeRepository employeeRepository;
    private final BiometricTemplateMapper biometricTemplateMapper;

    @Override
    @Transactional
    public BiometricTemplateResponse enrollTemplate(EnrollmentRequest request) {
        if (!employeeRepository.existsById(request.employeeId())) {
            throw new ResourceNotFoundException("Employee not found with id: " + request.employeeId());
        }

        if (request.qualityScore() != null && request.qualityScore() < MIN_QUALITY_SCORE) {
            throw new InvalidOperationException(
                    "Fingerprint quality too low (" + request.qualityScore() +
                            "). Please clean the scanner and re-scan.");
        }

        long existingFingerCount = biometricTemplateRepository.countByEmployeeId(request.employeeId());
        if (existingFingerCount >= MAX_FINGERS_PER_EMPLOYEE) {
            throw new InvalidOperationException(
                    "This employee already has " + MAX_FINGERS_PER_EMPLOYEE + " fingers enrolled");
        }

        boolean fingerAlreadyEnrolled = biometricTemplateRepository
                .existsByEmployeeIdAndFingerIndex(request.employeeId(), request.fingerIndex());
        if (fingerAlreadyEnrolled) {
            throw new InvalidOperationException(
                    "Finger index " + request.fingerIndex() + " is already enrolled for this employee");
        }

        BiometricDevice device = biometricDeviceRepository.findByDeviceSerial(request.deviceSerial())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Device not registered with serial: " + request.deviceSerial()));

        // Note: cross-employee duplicate detection (making sure this same
        // fingerprint isn't already enrolled to a DIFFERENT employee) is
        // performed by the bridge-agent itself against its local cache,
        // using the SecuGen SDK's real matching function — that comparison
        // requires the vendor SDK and cannot happen in plain backend code.
        // This service trusts that check and focuses on what it can safely
        // enforce: quality, per-employee finger limits, and storage.

        BiometricTemplate template = BiometricTemplate.builder()
                .employeeId(request.employeeId())
                .branchId(device.getBranchId())
                .templateData(Base64.getDecoder().decode(request.templateDataBase64()))
                .fingerIndex(request.fingerIndex())
                .qualityScore(request.qualityScore())
                .enrolledAt(LocalDateTime.now())
                .build();

        BiometricTemplate saved = biometricTemplateRepository.save(template);
        return biometricTemplateMapper.toBiometricTemplateResponse(saved);
    }

    @Override
    public List<TemplateSyncItemResponse> getTemplatesForDevice(String deviceSerial) {
        BiometricDevice device = biometricDeviceRepository.findByDeviceSerial(deviceSerial)
                .orElseThrow(() -> new ResourceNotFoundException("Unknown device: " + deviceSerial));

        device.setLastSyncAt(LocalDateTime.now());
        biometricDeviceRepository.save(device);

        return biometricTemplateRepository.findByBranchId(device.getBranchId()).stream()
                .map(biometricTemplateMapper::toTemplateSyncItemResponse)
                .toList();
    }
}