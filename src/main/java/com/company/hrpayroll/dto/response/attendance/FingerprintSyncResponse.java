package com.company.hrpayroll.dto.response.attendance;

import java.util.List;

// Pulled by a branch PC. It holds every template of the people working at that branch. employeeIds is the
// whole roster, so the PC can delete the local templates of anyone who has moved away or left.
// staffTemplates are the sign-in fingers of the branch's Branch Manager / Back Office logins: they only ever
// sign that login in on this PC, never record attendance. (Older PCs ignore the field.)
public record FingerprintSyncResponse(List<Item> templates, List<Long> employeeIds, List<StaffItem> staffTemplates) {

    public FingerprintSyncResponse(List<Item> templates, List<Long> employeeIds) {
        this(templates, employeeIds, List.of());
    }

    public FingerprintSyncResponse withStaffTemplates(List<StaffItem> staff) {
        return new FingerprintSyncResponse(templates, employeeIds, staff);
    }

    public record Item(
            Long employeeId,
            String terminalUserId,
            String employeeName,
            String employeeNumber,
            Short fingerSlot,
            String templateData,   // base64
            String templateHash,   // SHA-256 of the template, so the PC can skip what it already has
            Integer qualityScore
    ) {}

    public record StaffItem(
            Long userId,
            String terminalUserId,   // "staff-{userId}": how the PC names this finger, and sends it back to sign in
            String displayName,
            String role,             // BRANCH_MANAGER or BACK_OFFICE
            String templateData,
            String templateHash
    ) {}
}
