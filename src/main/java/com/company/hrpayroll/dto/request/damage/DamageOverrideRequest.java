// dto/request/damage/DamageOverrideRequest.java
package com.company.hrpayroll.dto.request.damage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Approving a damage without the employee's fingerprint is allowed, but never silently: the
// reason is mandatory and is kept on the record and in the audit trail.
public record DamageOverrideRequest(
        @NotBlank(message = "A reason is required to override fingerprint consent")
        @Size(min = 10, max = 500, message = "Reason must be between 10 and 500 characters") String reason
) {}
