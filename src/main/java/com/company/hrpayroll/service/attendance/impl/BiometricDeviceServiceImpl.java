// service/attendance/impl/BiometricDeviceServiceImpl.java
package com.company.hrpayroll.service.attendance.impl;

import com.company.hrpayroll.dto.request.attendance.DeviceRegistrationRequest;
import com.company.hrpayroll.dto.response.attendance.BiometricDeviceResponse;
import com.company.hrpayroll.dto.response.attendance.DeviceRegistrationResponse;
import com.company.hrpayroll.entity.attendance.BiometricDevice;
import com.company.hrpayroll.exception.DuplicateResourceException;
import com.company.hrpayroll.exception.ResourceNotFoundException;
import com.company.hrpayroll.mapper.attendance.BiometricDeviceMapper;
import com.company.hrpayroll.repository.attendance.BiometricDeviceRepository;
import com.company.hrpayroll.repository.branch.BranchRepository;
import com.company.hrpayroll.service.attendance.BiometricDeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BiometricDeviceServiceImpl implements BiometricDeviceService {

    private final BiometricDeviceRepository biometricDeviceRepository;
    private final BranchRepository branchRepository;
    private final BiometricDeviceMapper biometricDeviceMapper;
    private final PasswordEncoder passwordEncoder;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Override
    public DeviceRegistrationResponse registerDevice(DeviceRegistrationRequest request) {
        if (!branchRepository.existsById(request.branchId())) {
            throw new ResourceNotFoundException("Branch not found with id: " + request.branchId());
        }
        if (biometricDeviceRepository.findByDeviceSerial(request.deviceSerial()).isPresent()) {
            throw new DuplicateResourceException(
                    "A device with serial '" + request.deviceSerial() + "' is already registered");
        }

        String plaintextToken = generateToken();

        BiometricDevice device = BiometricDevice.builder()
                .branchId(request.branchId())
                .deviceSerial(request.deviceSerial())
                .deviceTokenHash(passwordEncoder.encode(plaintextToken))
                .status("ACTIVE")
                .build();

        BiometricDevice saved = biometricDeviceRepository.save(device);

        // The plaintext token is returned exactly once, here — install it
        // into the bridge-agent's local config immediately, as it cannot be
        // retrieved again.
        return biometricDeviceMapper.toDeviceRegistrationResponse(saved, plaintextToken);
    }

    @Override
    public List<BiometricDeviceResponse> getDevicesByBranch(Long branchId) {
        String branchName = branchRepository.findById(branchId)
                .map(b -> b.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found with id: " + branchId));

        return biometricDeviceRepository.findByBranchId(branchId).stream()
                .map(device -> biometricDeviceMapper.toBiometricDeviceResponse(device, branchName))
                .toList();
    }

    @Override
    public void deactivateDevice(Long deviceId) {
        BiometricDevice device = biometricDeviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with id: " + deviceId));
        device.setStatus("INACTIVE");
        biometricDeviceRepository.save(device);
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}