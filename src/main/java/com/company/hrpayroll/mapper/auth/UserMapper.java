// mapper/auth/UserMapper.java
package com.company.hrpayroll.mapper.auth;

import com.company.hrpayroll.dto.request.auth.CreateUserRequest;
import com.company.hrpayroll.dto.response.auth.UserSummaryResponse;
import com.company.hrpayroll.entity.auth.User;
import org.springframework.stereotype.Service;

@Service
public class UserMapper {

    // encodedPassword is passed in already-hashed by the service layer —
    // the mapper never touches raw passwords or the PasswordEncoder.
    public User toUser(CreateUserRequest dto, String encodedPassword) {
        return User.builder()
                .username(dto.username())
                .password(encodedPassword)
                .role(dto.role())
                .displayName(dto.displayName())
                .employeeId(dto.employeeId())
                .agentId(dto.agentId())
                .branchId(dto.branchId())
                .active(true)
                .mustChangePassword(true)
                .build();
    }

    public UserSummaryResponse toUserSummaryResponse(User user) {
        return new UserSummaryResponse(
                user.getId(),
                user.getUsername(),
                user.getRole().name(),
                user.getDisplayName(),
                user.getBranchId(),
                user.getAgentId(),
                user.isActive(),
                user.isCanRunPayments()
        );
    }
}