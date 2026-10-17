// dto/response/audit/AuditFilterOptionsResponse.java
package com.company.hrpayroll.dto.response.audit;

import java.util.List;

// The values the audit log can actually be filtered by — drives the filter dropdowns.
public record AuditFilterOptionsResponse(
        List<String> actions,
        List<String> entityTypes,
        List<UserOption> users
) {
    public record UserOption(Long id, String username) {}
}