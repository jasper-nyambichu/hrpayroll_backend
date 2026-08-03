// dto/response/attendance/TemplateSyncItemResponse.java
package com.company.hrpayroll.dto.response.attendance;

// Returned only over the device-authenticated sync channel — this is the
// one place raw template data is legitimately transmitted, to build the
// bridge-agent's local branch cache.
public record TemplateSyncItemResponse(
        Long employeeId,
        Integer fingerIndex,
        String templateDataBase64
) {}