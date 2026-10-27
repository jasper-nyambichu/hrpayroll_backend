// mapper/damage/DamageMapper.java
package com.company.hrpayroll.mapper.damage;

import com.company.hrpayroll.dto.request.damage.DamageRequest;
import com.company.hrpayroll.dto.response.damage.DamageResponse;
import com.company.hrpayroll.entity.damage.Damage;
import org.springframework.stereotype.Service;

import java.math.RoundingMode;
import java.time.LocalDate;

@Service
public class DamageMapper {

    public Damage toDamage(DamageRequest dto, Long branchId, Long recordedByUserId, LocalDate today) {
        return Damage.builder()
                .employeeId(dto.employeeId())
                .branchId(branchId)
                .amount(dto.amount().setScale(2, RoundingMode.HALF_UP))
                .description(dto.description().trim())
                .damageDate(dto.damageDate() != null ? dto.damageDate() : today)
                .status("PENDING_CONSENT")
                .recordedByUserId(recordedByUserId)
                .build();
    }

    // Names live in other tables, so the service resolves them and passes them in.
    public DamageResponse toResponse(
            Damage damage, String employeeName, String employeeNumber,
            String recordedByName, String overriddenByName) {
        return new DamageResponse(
                damage.getId(),
                damage.getEmployeeId(),
                employeeName,
                employeeNumber,
                damage.getBranchId(),
                damage.getAmount(),
                damage.getDescription(),
                damage.getDamageDate(),
                damage.getStatus(),
                damage.getRecordedByUserId(),
                recordedByName,
                damage.getApprovalMethod(),
                damage.getConsentAt(),
                damage.isOverridden(),
                damage.getOverrideReason(),
                overriddenByName,
                damage.getOverriddenAt(),
                damage.getPayrollRunId(),
                damage.getCreatedAt()
        );
    }
}
