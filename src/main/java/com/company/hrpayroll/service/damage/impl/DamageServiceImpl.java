// service/damage/impl/DamageServiceImpl.java
package com.company.hrpayroll.service.damage.impl;

import com.company.hrpayroll.config.BusinessClock;
import com.company.hrpayroll.dto.request.damage.BiometricConsentRequest;
import com.company.hrpayroll.dto.request.damage.DamageRequest;
import com.company.hrpayroll.dto.response.damage.DamageResponse;
import com.company.hrpayroll.dto.response.damage.PendingConsentResponse;
import com.company.hrpayroll.entity.attendance.BiometricDevice;
import com.company.hrpayroll.entity.auth.User;
import com.company.hrpayroll.entity.damage.Damage;
import com.company.hrpayroll.entity.employee.Employee;
import com.company.hrpayroll.exception.InvalidOperationException;
import com.company.hrpayroll.exception.ResourceNotFoundException;
import com.company.hrpayroll.mapper.damage.DamageMapper;
import com.company.hrpayroll.service.attendance.impl.TerminalIdentity;
import com.company.hrpayroll.repository.attendance.BiometricDeviceRepository;
import com.company.hrpayroll.repository.auth.UserRepository;
import com.company.hrpayroll.repository.damage.DamageRepository;
import com.company.hrpayroll.repository.employee.EmployeeRepository;
import com.company.hrpayroll.service.damage.DamageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DamageServiceImpl implements DamageService {

    private static final String PENDING = "PENDING_CONSENT";
    private static final String APPROVED = "APPROVED";
    private static final String CANCELLED = "CANCELLED";
    private static final String DEDUCTED = "DEDUCTED";

    private final DamageRepository damageRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final BiometricDeviceRepository biometricDeviceRepository;
    private final TerminalIdentity terminalIdentity;
    private final DamageMapper damageMapper;
    private final BusinessClock businessClock;

    @Override
    public DamageResponse recordDamage(DamageRequest request, Long recordedByUserId) {
        Employee employee = employeeRepository.findById(request.employeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + request.employeeId()));

        if (!"ACTIVE".equals(employee.getEmploymentStatus())) {
            throw new InvalidOperationException("A damage can only be recorded against an active employee");
        }

        Damage damage = damageMapper.toDamage(request, employee.getBranchId(), recordedByUserId, businessClock.today());
        return toResponse(damageRepository.save(damage));
    }

    @Override
    public DamageResponse overrideConsent(Long damageId, String reason, Long overriddenByUserId) {
        Damage damage = findOrThrow(damageId);
        if (!PENDING.equals(damage.getStatus())) {
            throw new InvalidOperationException("Only a damage awaiting consent can be overridden");
        }
        if (reason == null || reason.isBlank()) {
            throw new InvalidOperationException("A reason is required to override fingerprint consent");
        }

        damage.setStatus(APPROVED);
        damage.setApprovalMethod("OVERRIDE");
        damage.setOverridden(true);
        damage.setOverrideReason(reason.trim());
        damage.setOverriddenByUserId(overriddenByUserId);
        damage.setOverriddenAt(businessClock.now());
        return toResponse(damageRepository.save(damage));
    }

    @Override
    public DamageResponse cancelDamage(Long damageId) {
        Damage damage = findOrThrow(damageId);
        if (DEDUCTED.equals(damage.getStatus())) {
            throw new InvalidOperationException("A damage that has already been deducted cannot be cancelled");
        }
        if (CANCELLED.equals(damage.getStatus())) {
            throw new InvalidOperationException("This damage is already cancelled");
        }
        damage.setStatus(CANCELLED);
        return toResponse(damageRepository.save(damage));
    }

    @Override
    @Transactional
    public DamageResponse recordFingerprintConsent(BiometricConsentRequest request) {
        // A retried delivery of an event we already applied: answer with the same result.
        var already = damageRepository.findByConsentEventId(request.eventId());
        if (already.isPresent()) {
            return toResponse(already.get());
        }

        Damage damage = findOrThrow(request.damageId());
        if (!PENDING.equals(damage.getStatus())) {
            throw new InvalidOperationException("This damage is not awaiting consent");
        }

        BiometricDevice device = biometricDeviceRepository.findByDeviceSerial(request.deviceSerial())
                .orElseThrow(() -> new ResourceNotFoundException("Device not found: " + request.deviceSerial()));

        // The fingerprint must belong to the employee the damage was recorded against —
        // nobody else's scan can approve it.
        Long scannedEmployeeId = terminalIdentity.employeeFor(device.getId(), request.terminalUserId())
                .orElseThrow(() -> new InvalidOperationException("Unknown terminal user on this device"));

        if (!scannedEmployeeId.equals(damage.getEmployeeId())) {
            throw new InvalidOperationException("This fingerprint does not belong to the employee concerned");
        }

        damage.setStatus(APPROVED);
        damage.setApprovalMethod("FINGERPRINT");
        damage.setConsentAt(request.occurredAt() != null
                ? businessClock.resolve(null, request.occurredAt())
                : businessClock.now());
        damage.setConsentDeviceId(device.getId());
        damage.setConsentEventId(request.eventId());
        return toResponse(damageRepository.save(damage));
    }

    @Override
    public List<PendingConsentResponse> getPendingConsentsForDevice(String deviceSerial) {
        BiometricDevice device = biometricDeviceRepository.findByDeviceSerial(deviceSerial)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found: " + deviceSerial));

        // terminal user id by employee, for the enrolled employees who now work at this device's branch
        Map<Long, String> terminalUserByEmployee = terminalIdentity.idsAtBranchOf(device);

        if (terminalUserByEmployee.isEmpty()) {
            return List.of();
        }

        List<Damage> pending = damageRepository.findByEmployeeIdInAndStatus(
                List.copyOf(terminalUserByEmployee.keySet()), PENDING);
        Map<Long, Employee> employees = employeesById(pending);

        return pending.stream()
                .sorted((a, b) -> a.getId().compareTo(b.getId()))
                .map(damage -> {
                    Employee employee = employees.get(damage.getEmployeeId());
                    return new PendingConsentResponse(
                            damage.getId(),
                            terminalUserByEmployee.get(damage.getEmployeeId()),
                            damage.getEmployeeId(),
                            employee != null ? employee.getFullName() : null,
                            employee != null ? employee.getEmployeeNumber() : null,
                            damage.getAmount(),
                            damage.getDescription(),
                            damage.getDamageDate());
                })
                .toList();
    }

    @Override
    public List<DamageResponse> getDamages(String status, Long employeeId) {
        List<Damage> damages;
        boolean hasStatus = status != null && !status.isBlank();
        String normalisedStatus = hasStatus ? status.trim().toUpperCase() : null;

        if (employeeId != null && hasStatus) {
            damages = damageRepository.findByEmployeeIdAndStatus(employeeId, normalisedStatus);
        } else if (employeeId != null) {
            damages = damageRepository.findByEmployeeId(employeeId);
        } else if (hasStatus) {
            damages = damageRepository.findByStatus(normalisedStatus);
        } else {
            damages = damageRepository.findAll();
        }

        return damages.stream()
                .sorted((a, b) -> b.getId().compareTo(a.getId()))
                .map(this::toResponse)
                .toList();
    }

    @Override
    public DamageResponse getDamageById(Long damageId) {
        return toResponse(findOrThrow(damageId));
    }

    @Override
    public List<DamageDeductionResult> getApprovedDeductionsForEmployees(List<Long> employeeIds) {
        if (employeeIds == null || employeeIds.isEmpty()) {
            return List.of();
        }
        return damageRepository.findByEmployeeIdInAndStatus(employeeIds, APPROVED).stream()
                .map(damage -> new DamageDeductionResult(damage.getId(), damage.getEmployeeId(), damage.getAmount()))
                .toList();
    }

    @Override
    public void markDeducted(Long damageId, Long payrollRunId) {
        Damage damage = findOrThrow(damageId);
        if (!APPROVED.equals(damage.getStatus())) {
            throw new InvalidOperationException("Only an approved damage can be deducted");
        }
        damage.setStatus(DEDUCTED);
        damage.setPayrollRunId(payrollRunId);
        damage.setDeductedAt(businessClock.now());
        damageRepository.save(damage);
    }

    // ---- helpers ----

    private Damage findOrThrow(Long damageId) {
        return damageRepository.findById(damageId)
                .orElseThrow(() -> new ResourceNotFoundException("Damage not found with id: " + damageId));
    }

    private DamageResponse toResponse(Damage damage) {
        Employee employee = employeeRepository.findById(damage.getEmployeeId()).orElse(null);
        return damageMapper.toResponse(
                damage,
                employee != null ? employee.getFullName() : null,
                employee != null ? employee.getEmployeeNumber() : null,
                userName(damage.getRecordedByUserId()),
                userName(damage.getOverriddenByUserId()));
    }

    private String userName(Long userId) {
        if (userId == null) {
            return null;
        }
        return userRepository.findById(userId)
                .map(User::getDisplayName)
                .orElse(null);
    }

    private Map<Long, Employee> employeesById(List<Damage> damages) {
        List<Long> ids = damages.stream().map(Damage::getEmployeeId).distinct().toList();
        return employeeRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Employee::getId, Function.identity()));
    }
}
