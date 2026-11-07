// service/auth/AuthService.java
package com.company.hrpayroll.service.auth;

import com.company.hrpayroll.dto.request.auth.AgentLoginRequest;
import com.company.hrpayroll.dto.request.auth.EmployeeLoginRequest;
import com.company.hrpayroll.dto.request.auth.LoginRequest;
import com.company.hrpayroll.dto.request.auth.RefreshTokenRequest;
import com.company.hrpayroll.dto.response.auth.AuthResponse;

public interface AuthService {

    /** Staff surface: username + password. */
    AuthResponse login(LoginRequest request);

    /** Employee self-service surface: employee number + 4-digit PIN. */
    AuthResponse employeeLogin(EmployeeLoginRequest request);

    /** Agent portal surface: agent code + password. */
    AuthResponse agentLogin(AgentLoginRequest request);

    AuthResponse refresh(RefreshTokenRequest request);

    /** Branch PC surface: a Branch Manager or Back Office the PC recognised by fingerprint. Checked by StaffFingerprintService. */
    AuthResponse fingerprintLogin(com.company.hrpayroll.entity.auth.User user, String deviceSerial);
}
