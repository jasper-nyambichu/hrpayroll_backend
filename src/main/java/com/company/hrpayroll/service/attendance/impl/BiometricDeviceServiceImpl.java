// service/attendance/impl/BiometricDeviceServiceImpl.java
package com.company.hrpayroll.service.attendance.impl;

import com.company.hrpayroll.dto.request.attendance.DeviceRegistrationRequest;
import com.company.hrpayroll.dto.response.attendance.BiometricDeviceResponse;
import com.company.hrpayroll.dto.response.attendance.DeviceRegistrationResponse;
import com.company.hrpayroll.entity.attendance.BiometricDevice;
import com.company.hrpayroll.entity.branch.Branch;
import com.company.hrpayroll.exception.InvalidOperationException;
import com.company.hrpayroll.exception.ResourceNotFoundException;
import com.company.hrpayroll.repository.attendance.BiometricDeviceRepository;
import com.company.hrpayroll.repository.branch.BranchRepository;
import com.company.hrpayroll.service.attendance.BiometricDeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BiometricDeviceServiceImpl implements BiometricDeviceService {

    // 32 random bytes -> 43-char URL-safe base64 token. Generated once,
    // shown once in the response, never retrievable again — only the
    // PasswordEncoder hash is persisted, matching how DeviceAuthFilter
    // validates it later via passwordEncoder.matches(...).
    private static final int TOKEN_BYTES = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final BiometricDeviceRepository biometricDeviceRepository;
    private final BranchRepository branchRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public DeviceRegistrationResponse registerDevice(DeviceRegistrationRequest request) {
        if (!branchRepository.existsById(request.branchId())) {
            throw new ResourceNotFoundException("Branch not found with id: " + request.branchId());
        }

        boolean serialTaken = biometricDeviceRepository.findByDeviceSerial(request.deviceSerial()).isPresent();
        if (serialTaken) {
            throw new InvalidOperationException(
                    "A device is already registered with serial: " + request.deviceSerial());
        }

        String plaintextToken = generateToken();

        BiometricDevice device = BiometricDevice.builder()
                .branchId(request.branchId())
                .deviceSerial(request.deviceSerial())
                .deviceTokenHash(passwordEncoder.encode(plaintextToken))
                .status("ACTIVE")
                .build();

        BiometricDevice saved = biometricDeviceRepository.save(device);

        // plaintextToken only ever exists in this response — not persisted anywhere else
        return new DeviceRegistrationResponse(saved.getId(), saved.getDeviceSerial(), plaintextToken);
    }

    @Override
    public List<BiometricDeviceResponse> getDevicesByBranch(Long branchId) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found with id: " + branchId));

        return biometricDeviceRepository.findByBranchId(branchId).stream()
                .map(d -> new BiometricDeviceResponse(
                        d.getId(),
                        d.getBranchId(),
                        branch.getName(),
                        d.getDeviceSerial(),
                        d.getLastSyncAt(),
                        d.getStatus()
                ))
                .toList();
    }

    @Override
    @Transactional
    public void deactivateDevice(Long deviceId) {
        BiometricDevice device = biometricDeviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with id: " + deviceId));

        device.setStatus("INACTIVE");
        biometricDeviceRepository.save(device);
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}