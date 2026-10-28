// service/notification/NotificationRecipients.java
package com.company.hrpayroll.service.notification;

import com.company.hrpayroll.entity.auth.Role;
import com.company.hrpayroll.entity.auth.User;
import com.company.hrpayroll.repository.auth.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

// Who to tell when something needs a decision or has been decided: every active Admin, every active
// Accounts Office login, or the login that belongs to one agent. Shared by the loan, advance and
// payment modules so each does not keep its own copy of these lookups.
@Component
@RequiredArgsConstructor
public class NotificationRecipients {

    private final UserRepository userRepository;

    public List<User> admins() {
        return Stream.concat(
                        userRepository.findByRole(Role.ADMIN).stream(),
                        userRepository.findByRole(Role.SUPER_ADMIN).stream())
                .filter(User::isActive)
                .toList();
    }

    public List<User> accountsOffice() {
        return userRepository.findByRole(Role.FINANCE_OFFICER).stream()
                .filter(User::isActive)
                .toList();
    }

    public Optional<User> agentLogin(Long agentId) {
        if (agentId == null) {
            return Optional.empty();
        }
        return userRepository.findByAgentId(agentId).filter(User::isActive);
    }

    public String nameOf(Long userId) {
        if (userId == null) {
            return null;
        }
        return userRepository.findById(userId)
                .map((User u) -> u.getDisplayName() != null ? u.getDisplayName() : u.getUsername())
                .orElse(null);
    }
}
