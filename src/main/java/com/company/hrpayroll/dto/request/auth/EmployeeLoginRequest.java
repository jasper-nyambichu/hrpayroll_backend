// dto/request/auth/EmployeeLoginRequest.java
package com.company.hrpayroll.dto.request.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Employee self-service credential: the printed employee number plus a
 * 4-digit PIN.
 *
 * The PIN is the linked user account's password — stored BCrypt-hashed in
 * `users.password` like any other credential, so no separate PIN column and
 * no second hashing scheme. A 4-digit PIN is only 10,000 combinations, so
 * lockout on `users.active` is the only thing standing between an attacker
 * and a walk of the keyspace.
 */
public record EmployeeLoginRequest(
        @NotBlank(message = "Employee number is required") String employeeNumber,
        @NotBlank(message = "PIN is required")
        @Pattern(regexp = "\\d{4}", message = "PIN must be 4 digits") String pin
) {}
