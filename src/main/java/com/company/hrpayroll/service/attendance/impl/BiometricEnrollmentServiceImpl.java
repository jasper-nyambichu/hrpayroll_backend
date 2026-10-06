package com.company.hrpayroll.service.attendance.impl;

import com.company.hrpayroll.config.BusinessClock;
import com.company.hrpayroll.dto.request.attendance.EnrollmentRequest;
import com.company.hrpayroll.dto.response.attendance.DeviceMappingItemResponse;
import com.company.hrpayroll.entity.attendance.BiometricDevice;
import com.company.hrpayroll.entity.attendance.BiometricDeviceMapping;
import com.company.hrpayroll.entity.auth.Role;
import com.company.hrpayroll.entity.auth.User;
import com.company.hrpayroll.entity.branch.Branch;
import com.company.hrpayroll.entity.employee.Employee;
import com.company.hrpayroll.exception.InvalidOperationException;
import com.company.hrpayroll.exception.ResourceNotFoundException;
import com.company.hrpayroll.repository.attendance.BiometricDeviceMappingRepository;
import com.company.hrpayroll.repository.attendance.BiometricDeviceRepository;
import com.company.hrpayroll.repository.branch.BranchRepository;
import com.company.hrpayroll.repository.employee.EmployeeRepository;
import com.company.hrpayroll.service.attendance.BiometricEnrollmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

// Fingerprint capture and matching happen on the branch PC / reader. This service only records which
// employee a given terminal user id belongs to, and serves that list back (with names) to the kiosk.
@Service
@RequiredArgsConstructor
public class BiometricEnrollmentServiceImpl implements BiometricEnrollmentService {

    private final BiometricDeviceMappingRepository mappingRepository;
    private final BiometricDeviceRepository biometricDeviceRepository;
    private final EmployeeRepository employeeRepository;
    private final BranchRepository branchRepository;
    private final BusinessClock businessClock;

    @Override
    @Transactional
    public void enrollMapping(EnrollmentRequest request) {
        Employee employee = employeeRepository.findById(request.employeeId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee not found with id: " + request.employeeId()));

        BiometricDevice device = biometricDeviceRepository.findByDeviceSerial(request.deviceSerial())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Device not registered with serial: " + request.deviceSerial()));

        enforceBranchScope(device);

        // Fail now instead of creating a mapping every later scan would be rejected for.
        if (!employee.getBranchId().equals(device.getBranchId())) {
            throw new InvalidOperationException("Employee does not belong to the branch this device is registered to");
        }
        if (!"ACTIVE".equals(employee.getEmploymentStatus())) {
            throw new InvalidOperationException("Employee is not active — cannot be enrolled");
        }

        var existing = mappingRepository.findByDeviceIdAndTerminalUserId(device.getId(), request.terminalUserId());
        if (existing.isPresent()) {
            BiometricDeviceMapping mapping = existing.get();
            if (!mapping.getEmployeeId().equals(request.employeeId())) {
                throw new InvalidOperationException("Terminal user id " + request.terminalUserId()
                        + " is already mapped on this device to a different employee");
            }
            // Same person enrolling again (new PC, lost local data, re-capture): treat as success.
            mapping.setEnrolledAt(businessClock.now());
            mappingRepository.save(mapping);
            return;
        }

        BiometricDeviceMapping mapping = BiometricDeviceMapping.builder()
                .deviceId(device.getId())
                .terminalUserId(request.terminalUserId())
                .employeeId(request.employeeId())
                .enrolledAt(businessClock.now())
                .build();

        mappingRepository.save(mapping);
    }

    @Override
    @Transactional
    public List<DeviceMappingItemResponse> getMappingsForDevice(String deviceSerial) {
        BiometricDevice device = biometricDeviceRepository.findByDeviceSerial(deviceSerial)
                .orElseThrow(() -> new ResourceNotFoundException("Unknown device: " + deviceSerial));

        // Every successful pull doubles as a heartbeat for the "device online" indicator.
        device.setLastSyncAt(businessClock.now());
        biometricDeviceRepository.save(device);

        List<BiometricDeviceMapping> mappings = mappingRepository.findByDeviceId(device.getId());
        Map<Long, Employee> employees = employeeRepository
                .findAllById(mappings.stream().map(BiometricDeviceMapping::getEmployeeId).toList())
                .stream().collect(Collectors.toMap(Employee::getId, Function.identity()));
        String branchName = branchRepository.findById(device.getBranchId()).map(Branch::getName).orElse(null);

        return mappings.stream().map(m -> {
            Employee e = employees.get(m.getEmployeeId());
            return new DeviceMappingItemResponse(
                    m.getTerminalUserId(), m.getEmployeeId(),
                    e != null ? e.getFullName() : null,
                    e != null ? e.getEmployeeNumber() : null,
                    e != null ? e.getPosition() : null,
                    branchName);
        }).toList();
    }

    // A Branch Manager may only enroll on their own branch's device.
    private void enforceBranchScope(BiometricDevice device) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user
                && user.getRole() == Role.BRANCH_MANAGER
                && (user.getBranchId() == null || !user.getBranchId().equals(device.getBranchId()))) {
            throw new AccessDeniedException("You can only enroll employees on your own branch's device");
        }
    }
}