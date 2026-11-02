// service/auth/UserService.java
package com.company.hrpayroll.service.auth;

import com.company.hrpayroll.dto.request.auth.ChangePasswordRequest;
import com.company.hrpayroll.dto.request.auth.CreateUserRequest;
import com.company.hrpayroll.dto.response.auth.UserSummaryResponse;

import java.util.List;

public interface UserService {
    UserSummaryResponse createUser(CreateUserRequest request);
    List<UserSummaryResponse> findAllUsers();
    void changePassword(Long userId, ChangePasswordRequest request);
    void deactivateUser(Long userId);
    void deleteUser(Long userId, Long actingUserId);
    UserSummaryResponse assignBackOfficeBranch(Long userId, Long branchId);
}