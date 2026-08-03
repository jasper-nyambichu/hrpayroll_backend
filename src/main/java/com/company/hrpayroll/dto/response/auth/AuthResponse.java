// dto/response/auth/AuthResponse.java
package com.company.hrpayroll.dto.response.auth;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String role,
        String displayName,
        boolean mustChangePassword
) {}