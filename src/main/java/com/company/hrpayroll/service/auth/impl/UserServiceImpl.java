// service/auth/impl/UserServiceImpl.java
package com.company.hrpayroll.service.auth.impl;

import com.company.hrpayroll.config.BusinessClock;
import com.company.hrpayroll.dto.request.auth.ChangePasswordRequest;
import com.company.hrpayroll.dto.request.auth.CreateUserRequest;
import com.company.hrpayroll.dto.request.auth.CredentialsRequest;
import com.company.hrpayroll.dto.response.auth.SavedLoginResponse;
import com.company.hrpayroll.dto.response.auth.UserSummaryResponse;
import com.company.hrpayroll.entity.agent.Agent;
import com.company.hrpayroll.entity.auth.Role;
import com.company.hrpayroll.entity.auth.User;
import com.company.hrpayroll.entity.branch.Branch;
import com.company.hrpayroll.exception.DuplicateResourceException;
import com.company.hrpayroll.exception.InvalidCredentialsException;
import com.company.hrpayroll.exception.InvalidOperationException;
import com.company.hrpayroll.exception.ResourceNotFoundException;
import com.company.hrpayroll.mapper.auth.UserMapper;
import com.company.hrpayroll.repository.agent.AgentRepository;
import com.company.hrpayroll.repository.auth.UserRepository;
import com.company.hrpayroll.repository.branch.BranchRepository;
import com.company.hrpayroll.service.auth.UserService;
import com.company.hrpayroll.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    // The roles new accounts can be created for. Other roles remain only on deactivated accounts.
    private static final Set<Role> CREATABLE_ROLES = EnumSet.of(
            Role.ADMIN, Role.BRANCH_MANAGER, Role.AGENT, Role.BACK_OFFICE);

    // The logins whose username and password the Admin sets and shares.
    private static final Set<Role> ADMIN_SET_ROLES = EnumSet.of(Role.BRANCH_MANAGER, Role.BACK_OFFICE, Role.AGENT);

    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final AgentRepository agentRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;
    private final IssuedPasswordCipher issuedPasswordCipher;
    private final BusinessClock clock;

    @Override
    public UserSummaryResponse createUser(CreateUserRequest request) {
        if (!CREATABLE_ROLES.contains(request.role())) {
            throw new InvalidOperationException(
                    "New accounts can only be created for Admin, Branch Manager, Agent or Back Office roles");
        }

        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("Username '" + request.username() + "' is already taken");
        }

        String encodedPassword = passwordEncoder.encode(request.temporaryPassword());
        User user = userMapper.toUser(request, encodedPassword);
        User saved = userRepository.save(user);

        return userMapper.toUserSummaryResponse(saved);
    }

    @Override
    public List<UserSummaryResponse> findAllUsers() {
        return userRepository.findAll().stream()
                .filter(user -> !user.isDeleted())
                .map(userMapper::toUserSummaryResponse)
                .toList();
    }

    @Override
    public UserSummaryResponse findById(Long userId) {
        return userRepository.findById(userId)
                .map(userMapper::toUserSummaryResponse)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    @Override
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setMustChangePassword(false);
        user.forgetIssuedPassword(clock.now());
        userRepository.save(user);
    }

    // Moves a Branch Manager or Back Office login, all or nothing. Every request reloads the account, so
    // the old branch is closed to them from their very next request, without signing them out.
    // A branch keeps one login of each role, so a taken branch is refused unless the Admin asked to swap.
    @Override
    @Transactional
    public BranchMove assignBranch(Long userId, Long branchId, boolean swap) {
        User user = userRepository.findById(userId)
                .filter(found -> !found.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Role role = user.getRole();
        if (role != Role.BRANCH_MANAGER && role != Role.BACK_OFFICE) {
            throw new InvalidOperationException("Only a Branch Manager or Back Office login can be moved to a branch");
        }
        if (!user.isActive()) {
            throw new InvalidOperationException("This login is inactive, so it cannot be moved");
        }

        Branch target = branchRepository.findById(branchId)
                .filter(found -> !"DELETED".equals(found.getStatus()))
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found with id: " + branchId));
        if (!"ACTIVE".equals(target.getStatus())) {
            throw new InvalidOperationException("Branch '" + target.getName() + "' is inactive");
        }
        if (branchId.equals(user.getBranchId())) {
            throw new InvalidOperationException("This login already works at " + target.getName());
        }

        String title = role == Role.BRANCH_MANAGER ? "Branch Manager" : "Back Office";
        List<User> atTarget = userRepository.findByBranchId(branchId).stream()
                .filter(other -> other.getRole() == role && !other.getId().equals(userId))
                .toList();
        Branch current = user.getBranchId() == null ? null : branchRepository.findById(user.getBranchId()).orElse(null);

        if (!atTarget.isEmpty()) {
            boolean canSwap = swap && role == Role.BRANCH_MANAGER && current != null && atTarget.size() == 1;
            if (!canSwap) {
                throw new DuplicateResourceException(
                        target.getName() + " already has a " + title + ". Move or delete that login first.");
            }
            User other = atTarget.get(0);
            relocate(user, current, target, title);
            relocate(other, target, current, title);
            target.setManagerId(userId);
            current.setManagerId(other.getId());
            branchRepository.save(target);
            branchRepository.save(current);
            return new BranchMove(userMapper.toUserSummaryResponse(user), userMapper.toUserSummaryResponse(other));
        }

        if (role == Role.BRANCH_MANAGER) {
            branchRepository.findByManagerId(userId).forEach(previous -> {
                previous.setManagerId(null);
                branchRepository.save(previous);
            });
            target.setManagerId(userId);
            branchRepository.save(target);
        }
        relocate(user, current, target, title);
        return new BranchMove(userMapper.toUserSummaryResponse(user), null);
    }

    private void relocate(User user, Branch from, Branch to, String title) {
        // The automatic name carries the branch, so it moves too. A name the Admin chose is left alone.
        if (from != null && (from.getName() + " " + title).equals(user.getDisplayName())) {
            user.setDisplayName(to.getName() + " " + title);
        }
        user.setBranchId(to.getId());
        userRepository.save(user);

        notificationService.notifyInApp(user.getId(), "BRANCH_CHANGED",
                "You have been moved to " + to.getName() + ". You now work with that branch only.", to.getId());
    }

    // One Branch Manager and one Back Office login per branch. They need no password change at first
    // sign-in, because the Admin chose the password to share it.
    @Override
    @Transactional
    public UserSummaryResponse createBranchLogin(Long branchId, Role role, CredentialsRequest request) {
        if (role != Role.BRANCH_MANAGER && role != Role.BACK_OFFICE) {
            throw new InvalidOperationException("A branch login is either a Branch Manager or a Back Office");
        }

        Branch branch = branchRepository.findById(branchId)
                .filter(found -> !"DELETED".equals(found.getStatus()))
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found with id: " + branchId));
        rejectIfBranchHasLogin(branchId, role, null);

        String title = role == Role.BRANCH_MANAGER ? "Branch Manager" : "Back Office";
        User saved = userRepository.save(newLogin(request, role, branch.getName() + " " + title)
                .branchId(branchId)
                .build());
        rememberIssuedPassword(saved, request.password());
        saved = userRepository.save(saved);

        if (role == Role.BRANCH_MANAGER) {
            branch.setManagerId(saved.getId());
            branchRepository.save(branch);
        }
        return userMapper.toUserSummaryResponse(saved);
    }

    @Override
    @Transactional
    public UserSummaryResponse createAgentLogin(Long agentId, CredentialsRequest request) {
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent not found with id: " + agentId));
        if (!"ACTIVE".equals(agent.getStatus())) {
            throw new InvalidOperationException("This agent is inactive, so they cannot be given a login");
        }
        if (userRepository.findByAgentId(agentId).isPresent()) {
            throw new DuplicateResourceException("This agent already has a login. Change it instead.");
        }

        User saved = userRepository.save(newLogin(request, Role.AGENT, agent.getName())
                .agentId(agentId)
                .build());
        rememberIssuedPassword(saved, request.password());
        saved = userRepository.save(saved);
        return userMapper.toUserSummaryResponse(saved);
    }

    // A new username ends any session at once, because every request looks the account up by username.
    @Override
    public UserSummaryResponse changeCredentials(Long userId, CredentialsRequest request) {
        User user = userRepository.findById(userId)
                .filter(found -> !found.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (!ADMIN_SET_ROLES.contains(user.getRole())) {
            throw new InvalidOperationException("Only Branch Manager, Back Office and Agent logins can be changed here");
        }
        if (!request.username().equals(user.getUsername())) {
            rejectIfUsernameTaken(request.username());
            user.setUsername(request.username());
        }
        if (request.password() != null) {
            user.setPassword(passwordEncoder.encode(request.password()));
            user.setMustChangePassword(false);
            rememberIssuedPassword(user, request.password());
        }
        if (request.email() != null) {
            user.setEmail(checkedEmail(request.email(), userId));
        }
        return userMapper.toUserSummaryResponse(userRepository.save(user));
    }

    // What the Admin needs to share a login again later. A password the person chose themselves is never readable.
    @Override
    public SavedLoginResponse savedLogin(Long userId) {
        User user = userRepository.findById(userId)
                .filter(found -> !found.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        if (!ADMIN_SET_ROLES.contains(user.getRole())) {
            throw new InvalidOperationException("Only Branch Manager, Back Office and Agent logins are saved here");
        }
        String password = user.getIssuedPasswordEnc() == null ? null
                : issuedPasswordCipher.decrypt(user.getIssuedPasswordEnc(), user.getId());
        return new SavedLoginResponse(user.getId(), user.getUsername(), user.getEmail(), password,
                password == null ? null : user.getIssuedPasswordAt(), user.getPasswordChangedByOwnerAt());
    }

    private void rememberIssuedPassword(User user, String password) {
        user.setIssuedPasswordEnc(issuedPasswordCipher.encrypt(password, user.getId()));
        user.setIssuedPasswordAt(clock.now());
        user.setPasswordChangedByOwnerAt(null);
    }

    private User.UserBuilder newLogin(CredentialsRequest request, Role role, String displayName) {
        if (request.password() == null) {
            throw new InvalidOperationException("Password is required");
        }
        rejectIfUsernameTaken(request.username());

        return User.builder()
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .role(role)
                .displayName(displayName)
                .email(request.email() == null ? null : checkedEmail(request.email(), null))
                .active(true)
                .mustChangePassword(false);
    }

    // One address means one login, because a password reset can be asked for by email. Blank means none.
    private String checkedEmail(String email, Long exceptUserId) {
        if (email.isBlank()) {
            return null;
        }
        String trimmed = email.trim();
        userRepository.findByEmailIgnoreCaseAndDeletedFalse(trimmed)
                .filter(other -> !other.getId().equals(exceptUserId))
                .ifPresent(other -> {
                    throw new DuplicateResourceException("This email is already used by another login");
                });
        return trimmed;
    }

    private void rejectIfUsernameTaken(String username) {
        if (userRepository.existsByUsername(username)) {
            throw new DuplicateResourceException("Username '" + username + "' is already taken");
        }
    }

    private void rejectIfBranchHasLogin(Long branchId, Role role, Long exceptUserId) {
        boolean taken = userRepository.findByBranchId(branchId).stream()
                .anyMatch(user -> user.getRole() == role && !user.getId().equals(exceptUserId));
        if (taken) {
            String title = role == Role.BRANCH_MANAGER ? "Branch Manager" : "Back Office";
            throw new DuplicateResourceException("This branch already has a " + title + " login. Change it instead.");
        }
    }

    // The renamed username frees the old one and ends any session at once, because every request
    // looks the account up by username.
    @Override
    @Transactional
    public void deleteUser(Long userId, Long actingUserId) {
        User user = userRepository.findById(userId)
                .filter(found -> !found.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (userId.equals(actingUserId)) {
            throw new InvalidOperationException("You cannot delete your own account");
        }

        boolean admin = user.getRole() == Role.ADMIN || user.getRole() == Role.SUPER_ADMIN;
        long activeAdmins = Stream.concat(
                        userRepository.findByRole(Role.ADMIN).stream(),
                        userRepository.findByRole(Role.SUPER_ADMIN).stream())
                .filter(User::isActive)
                .count();
        if (admin && user.isActive() && activeAdmins <= 1) {
            throw new InvalidOperationException("This is the last active Admin account, so it cannot be deleted");
        }

        branchRepository.findByManagerId(userId).forEach(branch -> {
            branch.setManagerId(null);
            branchRepository.save(branch);
        });

        String renamed = "deleted-" + userId + "-" + user.getUsername();
        user.setUsername(renamed.length() > 100 ? renamed.substring(0, 100) : renamed);
        user.setDeleted(true);
        user.setActive(false);
        user.setEmployeeId(null);
        user.setAgentId(null);
        user.setBranchId(null);
        user.setEmail(null);
        userRepository.save(user);
    }

    @Override
    public void deactivateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        user.setActive(false);
        userRepository.save(user);
    }
}