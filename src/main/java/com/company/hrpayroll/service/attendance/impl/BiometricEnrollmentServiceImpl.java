package com.company.hrpayroll.service.attendance.impl;

import com.company.hrpayroll.config.BusinessClock;
import com.company.hrpayroll.dto.request.attendance.EnrollmentRequest;
import com.company.hrpayroll.dto.request.attendance.FingerprintEnrollRequest;
import com.company.hrpayroll.dto.response.attendance.DeviceMappingItemResponse;
import com.company.hrpayroll.dto.response.attendance.FingerprintSyncResponse;
import com.company.hrpayroll.entity.attendance.BiometricDevice;
import com.company.hrpayroll.entity.attendance.BiometricDeviceMapping;
import com.company.hrpayroll.entity.attendance.EmployeeFingerprint;
import com.company.hrpayroll.entity.auth.Role;
import com.company.hrpayroll.entity.auth.User;
import com.company.hrpayroll.entity.branch.Branch;
import com.company.hrpayroll.entity.employee.Employee;
import com.company.hrpayroll.exception.InvalidOperationException;
import com.company.hrpayroll.exception.ResourceNotFoundException;
import com.company.hrpayroll.repository.attendance.BiometricDeviceMappingRepository;
import com.company.hrpayroll.repository.attendance.BiometricDeviceRepository;
import com.company.hrpayroll.repository.attendance.EmployeeFingerprintRepository;
import com.company.hrpayroll.repository.branch.BranchRepository;
import com.company.hrpayroll.repository.employee.EmployeeRepository;
import com.company.hrpayroll.service.attendance.BiometricEnrollmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

// Fingerprint capture and matching happen on the branch PC / reader. This service only records which
// employee a given terminal user id belongs to, and serves that list back (with names) to the kiosk.
@Service
@RequiredArgsConstructor
public class BiometricEnrollmentServiceImpl implements BiometricEnrollmentService {

    private static final int MAX_TEMPLATE_BYTES = 2048;

    private final BiometricDeviceMappingRepository mappingRepository;
    private final EmployeeFingerprintRepository fingerprintRepository;
    private final FingerprintCipher fingerprintCipher;
    private final TerminalIdentity terminalIdentity;
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

        // Whoever works at this branch now, wherever they enrolled, so someone moved here is greeted by name.
        Map<Long, String> ids = terminalIdentity.idsAtBranchOf(device);
        Map<Long, Employee> employees = employeeRepository.findAllById(ids.keySet())
                .stream().collect(Collectors.toMap(Employee::getId, Function.identity()));
        String branchName = branchRepository.findById(device.getBranchId()).map(Branch::getName).orElse(null);

        return ids.entrySet().stream().map(entry -> {
            Employee e = employees.get(entry.getKey());
            return new DeviceMappingItemResponse(
                    entry.getValue(), entry.getKey(),
                    e != null ? e.getFullName() : null,
                    e != null ? e.getEmployeeNumber() : null,
                    e != null ? e.getPosition() : null,
                    branchName);
        }).toList();
    }

    // Saves the company-wide copy of a finger. It takes a signed-in operator rather than a device token,
    // because replacing a template decides whose finger clocks in as that employee.
    @Override
    @Transactional
    public void saveFingerprint(FingerprintEnrollRequest request) {
        Employee employee = employeeRepository.findById(request.employeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + request.employeeId()));
        BiometricDevice device = biometricDeviceRepository.findByDeviceSerial(request.deviceSerial())
                .orElseThrow(() -> new ResourceNotFoundException("Device not registered with serial: " + request.deviceSerial()));

        enforceBranchScope(device);
        if (!"ACTIVE".equals(employee.getEmploymentStatus())) {
            throw new InvalidOperationException("Employee is not active — cannot be enrolled");
        }

        byte[] template = decodeTemplate(request.templateData());
        linkTerminalId(employee, device, request.terminalUserId());

        short slot = request.fingerSlot();
        EmployeeFingerprint fingerprint = fingerprintRepository.findByEmployeeIdAndFingerSlot(employee.getId(), slot)
                .orElseGet(() -> EmployeeFingerprint.builder().employeeId(employee.getId()).fingerSlot(slot).build());
        fingerprint.setTemplateEncrypted(fingerprintCipher.encrypt(template, employee.getId(), slot));
        fingerprint.setQualityScore(request.qualityScore());
        fingerprint.setEnrolledDeviceId(device.getId());
        fingerprintRepository.save(fingerprint);
    }

    // One terminal id means one person across the company. A PC backing up a template it already holds
    // may still do so after that employee has moved away, but a new link needs them to work at this branch.
    private void linkTerminalId(Employee employee, BiometricDevice device, String terminalUserId) {
        boolean takenByOther = mappingRepository.findByTerminalUserId(terminalUserId).stream()
                .anyMatch(row -> !row.getEmployeeId().equals(employee.getId()));
        if (takenByOther) {
            throw new InvalidOperationException("Terminal user id " + terminalUserId + " already belongs to a different employee");
        }
        if (mappingRepository.existsByDeviceIdAndTerminalUserId(device.getId(), terminalUserId)) {
            return;
        }
        if (!employee.getBranchId().equals(device.getBranchId())) {
            throw new InvalidOperationException("Employee does not belong to the branch this device is registered to");
        }
        mappingRepository.save(BiometricDeviceMapping.builder()
                .deviceId(device.getId())
                .terminalUserId(terminalUserId)
                .employeeId(employee.getId())
                .enrolledAt(businessClock.now())
                .build());
    }

    private static byte[] decodeTemplate(String base64) {
        byte[] template;
        try {
            template = Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException ex) {
            throw new InvalidOperationException("The fingerprint template could not be read");
        }
        if (template.length == 0 || template.length > MAX_TEMPLATE_BYTES) {
            throw new InvalidOperationException("The fingerprint template could not be read");
        }
        return template;
    }

    // A branch PC receives only the people working at its branch. The device is the authenticated one.
    @Override
    @Transactional
    public FingerprintSyncResponse getFingerprintsForDevice(String deviceSerial) {
        BiometricDevice device = biometricDeviceRepository.findByDeviceSerial(deviceSerial)
                .orElseThrow(() -> new ResourceNotFoundException("Unknown device: " + deviceSerial));
        device.setLastSyncAt(businessClock.now());
        biometricDeviceRepository.save(device);

        List<Employee> staff = employeeRepository.findByBranchId(device.getBranchId()).stream()
                .filter(employee -> "ACTIVE".equals(employee.getEmploymentStatus()))
                .toList();
        List<Long> staffIds = staff.stream().map(Employee::getId).toList();
        Map<Long, Employee> byId = staff.stream().collect(Collectors.toMap(Employee::getId, Function.identity()));
        Map<Long, String> terminalIds = terminalIdentity.idsAtBranchOf(device);

        List<FingerprintSyncResponse.Item> items = new ArrayList<>();
        if (!staffIds.isEmpty()) {
            for (EmployeeFingerprint row : fingerprintRepository.findByEmployeeIdIn(staffIds)) {
                String terminalId = terminalIds.get(row.getEmployeeId());
                if (terminalId == null) {
                    continue;
                }
                Employee employee = byId.get(row.getEmployeeId());
                byte[] template = fingerprintCipher.decrypt(row.getTemplateEncrypted(), row.getEmployeeId(), row.getFingerSlot());
                items.add(new FingerprintSyncResponse.Item(
                        row.getEmployeeId(), terminalId, employee.getFullName(), employee.getEmployeeNumber(),
                        row.getFingerSlot(), Base64.getEncoder().encodeToString(template), sha256(template), row.getQualityScore()));
            }
        }
        return new FingerprintSyncResponse(items, staffIds);
    }

    @Override
    public long countFingerprints(Long employeeId) {
        return fingerprintRepository.countByEmployeeId(employeeId);
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
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