package com.company.hrpayroll.dto.response.attendance;

import java.util.List;

// Pulled by a branch PC. It holds every template of the people working at that branch. employeeIds is the
// whole roster, so the PC can delete the local templates of anyone who has moved away or left.
public record FingerprintSyncResponse(List<Item> templates, List<Long> employeeIds) {

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
}
