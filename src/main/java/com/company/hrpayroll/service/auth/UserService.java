// service/auth/UserService.java
package com.company.hrpayroll.service.auth;

import com.company.hrpayroll.dto.request.auth.ChangePasswordRequest;
import com.company.hrpayroll.dto.request.auth.CreateUserRequest;
import com.company.hrpayroll.dto.request.auth.CredentialsRequest;
import com.company.hrpayroll.dto.response.auth.UserSummaryResponse;
import com.company.hrpayroll.entity.auth.Role;

import java.util.List;

public interface UserService {
    UserSummaryResponse createUser(CreateUserRequest request);
    List<UserSummaryResponse> findAllUsers();
    UserSummaryResponse findById(Long userId);
    void changePassword(Long userId, ChangePasswordRequest request);
    void deactivateUser(Long userId);
    void deleteUser(Long userId, Long actingUserId);
    // With swap, a Branch Manager and the one already at the branch trade places. Who moved is returned for the history.
    BranchMove assignBranch(Long userId, Long branchId, boolean swap);

    record BranchMove(UserSummaryResponse moved, UserSummaryResponse swappedWith) {}
    UserSummaryResponse createBranchLogin(Long branchId, Role role, CredentialsRequest request);
    UserSummaryResponse createAgentLogin(Long agentId, CredentialsRequest request);
    UserSummaryResponse changeCredentials(Long userId, CredentialsRequest request);
}