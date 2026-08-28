package com.company.hrpayroll.service.attendance.impl;

import com.company.hrpayroll.dto.request.attendance.EnrollmentRequest;
import com.company.hrpayroll.dto.response.attendance.DeviceMappingItemResponse;
import com.company.hrpayroll.entity.attendance.BiometricDevice;
import com.company.hrpayroll.entity.attendance.BiometricDeviceMapping;
import com.company.hrpayroll.exception.InvalidOperationException;
import com.company.hrpayroll.exception.ResourceNotFoundException;
import com.company.hrpayroll.repository.attendance.BiometricDeviceMappingRepository;
import com.company.hrpayroll.repository.attendance.BiometricDeviceRepository;
import com.company.hrpayroll.repository.employee.EmployeeRepository;
import com.company.hrpayroll.service.attendance.BiometricEnrollmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

// Enrollment (fingerprint capture, quality checks, duplicate-finger
// rejection) is handled entirely by the ZKTeco terminal's own firmware now.
// This service's only remaining job is recording which employee a given
// terminal user id belongs to.
@Service
@RequiredArgsConstructor
public class BiometricEnrollmentServiceImpl implements BiometricEnrollmentService {

    private final BiometricDeviceMappingRepository mappingRepository;
    private final BiometricDeviceRepository biometricDeviceRepository;
    private final EmployeeRepository employeeRepository;

    @Override
    @Transactional
    public void enrollMapping(EnrollmentRequest request) {
        if (!employeeRepository.existsById(request.employeeId())) {
            throw new ResourceNotFoundException("Employee not found with id: " + request.employeeId());
        }

        BiometricDevice device = biometricDeviceRepository.findByDeviceSerial(request.deviceSerial())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Device not registered with serial: " + request.deviceSerial()));

        boolean alreadyMapped = mappingRepository
                .existsByDeviceIdAndTerminalUserId(device.getId(), request.terminalUserId());
        if (alreadyMapped) {
            throw new InvalidOperationException(
                    "Terminal user id " + request.terminalUserId() +
                            " is already mapped on this device");
        }

        BiometricDeviceMapping mapping = BiometricDeviceMapping.builder()
                .deviceId(device.getId())
                .terminalUserId(request.terminalUserId())
                .employeeId(request.employeeId())
                .enrolledAt(LocalDateTime.now())
                .build();

        mappingRepository.save(mapping);
    }

    @Override
    public List<DeviceMappingItemResponse> getMappingsForDevice(String deviceSerial) {
        BiometricDevice device = biometricDeviceRepository.findByDeviceSerial(deviceSerial)
                .orElseThrow(() -> new ResourceNotFoundException("Unknown device: " + deviceSerial));

        device.setLastSyncAt(LocalDateTime.now());
        biometricDeviceRepository.save(device);

        return mappingRepository.findByDeviceId(device.getId()).stream()
                .map(m -> new DeviceMappingItemResponse(m.getTerminalUserId(), m.getEmployeeId()))
                .toList();
    }
}