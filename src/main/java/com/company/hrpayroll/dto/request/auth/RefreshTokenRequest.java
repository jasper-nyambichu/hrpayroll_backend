// dto/request/auth/RefreshTokenRequest.java
package com.company.hrpayroll.dto.request.auth;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
        @NotBlank(message = "Refresh token is required") String refreshToken
) {}