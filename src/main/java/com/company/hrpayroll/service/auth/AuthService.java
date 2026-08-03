// service/auth/AuthService.java
package com.company.hrpayroll.service.auth;

import com.company.hrpayroll.dto.request.auth.LoginRequest;
import com.company.hrpayroll.dto.request.auth.RefreshTokenRequest;
import com.company.hrpayroll.dto.response.auth.AuthResponse;

public interface AuthService {
    AuthResponse login(LoginRequest request);
    AuthResponse refresh(RefreshTokenRequest request);
}