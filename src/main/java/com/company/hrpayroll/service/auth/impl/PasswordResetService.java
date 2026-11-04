package com.company.hrpayroll.service.auth.impl;

import com.company.hrpayroll.audit.AuditDetails;
import com.company.hrpayroll.config.BusinessClock;
import com.company.hrpayroll.entity.auth.PasswordReset;
import com.company.hrpayroll.entity.auth.Role;
import com.company.hrpayroll.entity.auth.User;
import com.company.hrpayroll.exception.InvalidOperationException;
import com.company.hrpayroll.repository.auth.PasswordResetRepository;
import com.company.hrpayroll.repository.auth.UserRepository;
import com.company.hrpayroll.security.LoginAttemptService;
import com.company.hrpayroll.service.audit.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Forgotten passwords: username or email, then an emailed code, then a new password.
 *
 * The first step answers the same way for every identifier, so it cannot be used to discover who has an
 * account. Clicking the emailed link only proves something to the browser that clicked it: nothing here
 * tells the tab that asked for the code, because that tab may belong to someone else.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final int CODE_MINUTES = 10;
    private static final int TOKEN_MINUTES = 15;
    private static final int MAX_CODE_ATTEMPTS = 5;
    private static final String WRONG_CODE = "That code is wrong or has expired. Check it, or ask for a new one.";

    private static final Set<Role> PORTAL_ROLES = EnumSet.of(
            Role.SUPER_ADMIN, Role.ADMIN, Role.BRANCH_MANAGER, Role.AGENT, Role.BACK_OFFICE);

    private final UserRepository userRepository;
    private final PasswordResetRepository resetRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetMailer mailer;
    private final LoginAttemptService loginAttempts;
    private final AuditService auditService;
    private final BusinessClock clock;

    private final SecureRandom random = new SecureRandom();

    @Transactional
    public void requestCode(String identifier) {
        String key = "forgot:" + normalise(identifier);
        // Each request counts, so nobody can flood someone's inbox. Still answered the same way.
        if (loginAttempts.isLocked(key)) {
            return;
        }
        loginAttempts.recordFailure(key);

        Optional<User> found = findAccount(identifier);
        if (found.isEmpty()) {
            return;
        }
        User user = found.get();
        if (user.getEmail() == null) {
            log.warn("Password reset asked for user {}, who has no email address", user.getId());
            return;
        }

        LocalDateTime now = clock.now();
        resetRepository.findByUserIdAndUsedAtIsNull(user.getId()).forEach(old -> {
            old.setUsedAt(now);
            resetRepository.save(old);
        });

        String code = "%06d".formatted(random.nextInt(1_000_000));
        resetRepository.save(PasswordReset.builder()
                .userId(user.getId())
                .codeHash(passwordEncoder.encode(code))
                .expiresAt(now.plusMinutes(CODE_MINUTES))
                .build());

        auditService.record(user.getId(), "PASSWORD_RESET_REQUESTED", "User", user.getId(), AuditDetails.withRequest());
        mailer.sendCode(user.getEmail(), nameOf(user), user.getUsername(), code, CODE_MINUTES);
    }

    // Returns the token that allows setting a new password. It only ever goes to whoever holds the code.
    @Transactional
    public String verifyCode(String identifier, String code) {
        String key = "reset-code:" + normalise(identifier);
        if (loginAttempts.isLocked(key)) {
            throw new InvalidOperationException("Too many wrong codes. Wait 15 minutes, then ask for a new one.");
        }

        LocalDateTime now = clock.now();
        Optional<PasswordReset> found = findAccount(identifier)
                .flatMap(user -> resetRepository.findFirstByUserIdAndUsedAtIsNullOrderByIdDesc(user.getId()))
                .filter(reset -> reset.getExpiresAt().isAfter(now));

        if (found.isEmpty() || !passwordEncoder.matches(code, found.get().getCodeHash())) {
            loginAttempts.recordFailure(key);
            found.ifPresent(reset -> {
                reset.setAttempts(reset.getAttempts() + 1);
                if (reset.getAttempts() >= MAX_CODE_ATTEMPTS) {
                    reset.setUsedAt(now);
                }
                resetRepository.save(reset);
            });
            throw new InvalidOperationException(WRONG_CODE);
        }

        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        PasswordReset reset = found.get();
        reset.setResetTokenHash(sha256(token));
        reset.setResetTokenExpiresAt(now.plusMinutes(TOKEN_MINUTES));
        resetRepository.save(reset);
        loginAttempts.recordSuccess(key);
        return token;
    }

    // Returns the role, so the screen can send them to the right login page.
    @Transactional
    public String resetPassword(String token, String newPassword) {
        LocalDateTime now = clock.now();
        PasswordReset reset = resetRepository.findByResetTokenHashAndUsedAtIsNull(sha256(token))
                .filter(found -> found.getResetTokenExpiresAt() != null && found.getResetTokenExpiresAt().isAfter(now))
                .orElseThrow(() -> new InvalidOperationException("This reset has expired. Please start again."));

        User user = userRepository.findById(reset.getUserId())
                .filter(found -> !found.isDeleted() && found.isActive())
                .orElseThrow(() -> new InvalidOperationException("This reset has expired. Please start again."));

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        userRepository.save(user);

        reset.setUsedAt(now);
        resetRepository.save(reset);

        // They proved who they are, so an earlier lockout no longer stands in their way.
        loginAttempts.recordSuccess("login:" + user.getUsername());
        loginAttempts.recordSuccess("agent:" + user.getUsername());

        auditService.record(user.getId(), "PASSWORD_RESET_COMPLETED", "User", user.getId(), AuditDetails.withRequest());
        if (user.getEmail() != null) {
            mailer.sendPasswordChanged(user.getEmail(), nameOf(user));
        }
        return user.getRole().name();
    }

    private Optional<User> findAccount(String identifier) {
        String value = identifier.trim();
        return userRepository.findByUsername(value)
                .or(() -> userRepository.findByEmailIgnoreCaseAndDeletedFalse(value))
                .filter(user -> user.isActive() && !user.isDeleted() && PORTAL_ROLES.contains(user.getRole()));
    }

    private static String nameOf(User user) {
        return user.getDisplayName() != null ? user.getDisplayName() : user.getUsername();
    }

    private static String normalise(String identifier) {
        return identifier.trim().toLowerCase(Locale.ROOT);
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
