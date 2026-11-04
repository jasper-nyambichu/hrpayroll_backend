// dto/request/auth/CredentialsRequest.java
package com.company.hrpayroll.dto.request.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// The login the Admin sets for a branch or an agent. A new login must have a password; when changing
// one, leaving the password out keeps the current one. 50 is the most the agent login accepts.
public record CredentialsRequest(
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be 3 to 50 characters")
        @Pattern(regexp = "^[A-Za-z0-9._@-]*$", message = "Username can only use letters, numbers and . _ @ -")
        String username,

        @Size(min = 8, max = 72, message = "Password must be 8 to 72 characters")
        @Pattern(regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d).*$",
                message = "Password needs an uppercase letter, a lowercase letter and a number")
        String password,

        // Where a forgotten-password code is sent. Left out keeps the current one; blank removes it.
        @Email(message = "Enter a valid email address")
        @Size(max = 150, message = "That email address is too long")
        String email
) {}
