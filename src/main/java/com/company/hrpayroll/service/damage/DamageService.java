// service/damage/DamageService.java
package com.company.hrpayroll.service.damage;

import com.company.hrpayroll.dto.request.damage.BiometricConsentRequest;
import com.company.hrpayroll.dto.request.damage.DamageRequest;
import com.company.hrpayroll.dto.response.damage.DamageResponse;
import com.company.hrpayroll.dto.response.damage.PendingConsentResponse;

import java.math.BigDecimal;
import java.util.List;

public interface DamageService {

    // Starts PENDING_CONSENT: nothing is deducted until the employee agrees or an officer overrides.
    DamageResponse recordDamage(DamageRequest request, Long recordedByUserId);

    // Approves without the employee's fingerprint. The reason is mandatory; the record is flagged
    // overridden for good.
    DamageResponse overrideConsent(Long damageId, String reason, Long overriddenByUserId);

    // Withdraws a damage that has not been deducted yet.
    DamageResponse cancelDamage(Long damageId);

    // Called from the device-authenticated endpoint — the employee agreed at the scanner.
    DamageResponse recordFingerprintConsent(BiometricConsentRequest request);

    // For the desktop application: what is waiting for a fingerprint on this device.
    List<PendingConsentResponse> getPendingConsentsForDevice(String deviceSerial);

    // Both filters optional. Newest first.
    List<DamageResponse> getDamages(String status, Long employeeId);
    List<DamageResponse> getDamagesForBranch(String status, Long employeeId, Long branchId);

    DamageResponse getDamageById(Long damageId);

    // Called by payroll/engine during a run — not exposed via a controller.
    List<DamageDeductionResult> getApprovedDeductionsForEmployees(List<Long> employeeIds);
    void markDeducted(Long damageId, Long payrollRunId);

    record DamageDeductionResult(Long damageId, Long employeeId, BigDecimal amount) {}
}
