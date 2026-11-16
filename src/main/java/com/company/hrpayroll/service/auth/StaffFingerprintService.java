// service/auth/StaffFingerprintService.java
package com.company.hrpayroll.service.auth;

import com.company.hrpayroll.dto.response.auth.AuthResponse;
import com.company.hrpayroll.dto.response.auth.UserSummaryResponse;
import com.company.hrpayroll.entity.attendance.BiometricDevice;
import com.company.hrpayroll.entity.auth.Role;
import com.company.hrpayroll.entity.auth.User;
import com.company.hrpayroll.entity.employee.Employee;
import com.company.hrpayroll.exception.InvalidOperationException;
import com.company.hrpayroll.exception.ResourceNotFoundException;
import com.company.hrpayroll.mapper.auth.UserMapper;
import com.company.hrpayroll.repository.attendance.BiometricDeviceRepository;
import com.company.hrpayroll.repository.auth.UserRepository;
import com.company.hrpayroll.repository.employee.EmployeeRepository;
import com.company.hrpayroll.service.attendance.impl.TerminalIdentity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.company.hrpayroll.dto.response.attendance.FingerprintSyncResponse;
import com.company.hrpayroll.entity.auth.LoginFingerprint;
import com.company.hrpayroll.repository.auth.LoginFingerprintRepository;
import com.company.hrpayroll.service.attendance.impl.FingerprintCipher;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A Branch Manager or Back Office login can be linked to that person's own employee record, because
 * they may also work, and clock in, as an employee of the branch. The link lets the branch PC:
 *   - mark that person in its lists ("Branch Manager"), and
 *   - sign them in with their fingerprint instead of a password.
 *
 * Fingerprint sign-in trusts the branch PC: the finger is matched on the PC, and the PC proves who it is
 * with its device token (like a clock-in or a damage consent). So it only ever works on an activated PC
 * of the person's own branch, for a login linked to an employee working at that branch, and every
 * sign-in is audited. The password stays the way to sign in anywhere else, and to activate a new PC.
 */
@Service
@RequiredArgsConstructor
public class StaffFingerprintService {

    public static final Set<Role> LINKABLE_ROLES = Set.of(Role.BRANCH_MANAGER, Role.BACK_OFFICE);

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final BiometricDeviceRepository deviceRepository;
    private final TerminalIdentity terminalIdentity;
    private final UserMapper userMapper;
    private final AuthService authService;
    private final LoginFingerprintRepository loginFingerprintRepository;
    private final FingerprintCipher fingerprintCipher;

    /** Admin: links (or, with null, unlinks) a Branch Manager or Back Office login to an employee record. */
    @Transactional
    public UserSummaryResponse linkEmployee(Long userId, Long employeeId) {
        User user = userRepository.findById(userId)
                .filter(found -> !found.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        if (!LINKABLE_ROLES.contains(user.getRole())) {
            throw new InvalidOperationException("Only a Branch Manager or Back Office login can be linked to an employee");
        }

        if (employeeId == null) {
            user.setEmployeeId(null);
            return userMapper.toUserSummaryResponse(userRepository.save(user));
        }

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));
        if (user.getBranchId() == null || !user.getBranchId().equals(employee.getBranchId())) {
            throw new InvalidOperationException("Choose an employee who works at this login's branch");
        }
        if (!"ACTIVE".equals(employee.getEmploymentStatus())) {
            throw new InvalidOperationException("Choose an active employee");
        }
        boolean takenByAnother = userRepository.findByEmployeeIdIn(List.of(employeeId)).stream()
                .anyMatch(other -> !other.getId().equals(userId) && !other.isDeleted());
        if (takenByAnother) {
            throw new InvalidOperationException("That employee is already linked to another login");
        }

        user.setEmployeeId(employeeId);
        return userMapper.toUserSummaryResponse(userRepository.save(user));
    }

    /** employee id -> BRANCH_MANAGER or BACK_OFFICE, for active logins of this branch linked to an employee. */
    public Map<Long, String> staffRolesAtBranch(Long branchId) {
        if (branchId == null) {
            return Map.of();
        }
        return userRepository.findByBranchId(branchId).stream()
                .filter(user -> user.isActive() && !user.isDeleted() && user.getEmployeeId() != null)
                .filter(user -> LINKABLE_ROLES.contains(user.getRole()))
                .collect(Collectors.toMap(User::getEmployeeId, user -> user.getRole().name(), (a, b) -> a));
    }

    /** staffRolesAtBranch for the branch of an authenticated branch PC. */
    public Map<Long, String> staffRolesForDevice(String deviceSerial) {
        return deviceRepository.findByDeviceSerial(deviceSerial)
                .map(device -> staffRolesAtBranch(device.getBranchId()))
                .orElse(Map.of());
    }

    /**
     * Signs in the Branch Manager or Back Office whose finger the branch PC just recognised. The device
     * serial comes from the authenticated device token, never from the request body.
     */
    public AuthResponse signIn(String deviceSerial, String terminalUserId, String requestedRole) {
        if (terminalUserId != null && terminalUserId.startsWith(LOGIN_FINGER_PREFIX)) {
            return signInWithLoginFinger(deviceSerial, terminalUserId, requestedRole);
        }
        // "STAFF": whichever of the two logins this finger is linked to (the branch PC has one staff sign-in
        // for the Back Office and the Branch Manager). A named role still works for older PCs.
        Role role = null;
        if (!"STAFF".equals(requestedRole)) {
            try {
                role = Role.valueOf(requestedRole);
            } catch (IllegalArgumentException | NullPointerException e) {
                throw new InvalidOperationException("Unknown role");
            }
            if (!LINKABLE_ROLES.contains(role)) {
                throw new InvalidOperationException("Fingerprint sign-in is for the Branch Manager and the Back Office only");
            }
        }
        final Role wanted = role;

        BiometricDevice device = deviceRepository.findByDeviceSerial(deviceSerial)
                .filter(found -> "ACTIVE".equals(found.getStatus()))
                .orElseThrow(() -> new InvalidOperationException("This PC's reader is not active"));

        Long employeeId = terminalIdentity.employeeFor(device.getId(), terminalUserId).orElse(null);
        Employee employee = employeeId == null ? null : employeeRepository.findById(employeeId).orElse(null);
        if (employee == null || !Objects.equals(employee.getBranchId(), device.getBranchId())) {
            throw new InvalidOperationException(notLinked(wanted));
        }

        User user = userRepository.findByEmployeeIdIn(List.of(employeeId)).stream()
                .filter(found -> (wanted == null ? LINKABLE_ROLES.contains(found.getRole()) : found.getRole() == wanted)
                        && found.isActive() && !found.isDeleted())
                .filter(found -> Objects.equals(found.getBranchId(), device.getBranchId()))
                .findFirst()
                .orElseThrow(() -> new InvalidOperationException(notLinked(wanted)));

        return authService.fingerprintLogin(user, deviceSerial);
    }

    // ===================================================================================================
    //  The sign-in finger of a login itself, for a Branch Manager (or Back Office) who is not an employee.
    // ===================================================================================================

    /** How a branch PC names a login's sign-in finger: never an employee's terminal id ("emp-…"). */
    public static final String LOGIN_FINGER_PREFIX = "staff-";
    private static final int MAX_TEMPLATE_BYTES = 2048;

    /** Whether this login has a sign-in finger registered. */
    public boolean hasLoginFingerprint(Long userId) {
        return loginFingerprintRepository.existsByUserId(userId);
    }

    /**
     * The signed-in Branch Manager or Back Office registers (or replaces) their OWN sign-in finger, on an active PC
     * of their own branch. Having just signed in proves who they are, so nobody can put a finger on someone else's login.
     */
    @Transactional
    public void saveOwnLoginFingerprint(User current, String deviceSerial, String templateBase64, Integer qualityScore) {
        if (current == null || !LINKABLE_ROLES.contains(current.getRole())) {
            throw new InvalidOperationException("Only a Branch Manager or Back Office can register a sign-in fingerprint");
        }
        BiometricDevice device = deviceRepository.findByDeviceSerial(deviceSerial)
                .filter(found -> "ACTIVE".equals(found.getStatus()))
                .orElseThrow(() -> new InvalidOperationException("This PC's reader is not active"));
        if (current.getBranchId() == null || !current.getBranchId().equals(device.getBranchId())) {
            throw new InvalidOperationException("Register your fingerprint on a PC of your own branch");
        }

        byte[] template;
        try {
            template = Base64.getDecoder().decode(templateBase64);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new InvalidOperationException("The fingerprint template could not be read");
        }
        if (template.length == 0 || template.length > MAX_TEMPLATE_BYTES) {
            throw new InvalidOperationException("The fingerprint template could not be read");
        }

        LoginFingerprint row = loginFingerprintRepository.findByUserId(current.getId())
                .orElseGet(() -> LoginFingerprint.builder().userId(current.getId()).build());
        row.setTemplateEncrypted(fingerprintCipher.encryptForLogin(template, current.getId()));
        row.setQualityScore(qualityScore);
        row.setEnrolledDeviceId(device.getId());
        loginFingerprintRepository.save(row);
    }

    /** The sign-in fingers of the active Branch Manager / Back Office logins of this PC's branch. */
    @Transactional(readOnly = true)
    public List<FingerprintSyncResponse.StaffItem> loginFingerprintsForDevice(String deviceSerial) {
        BiometricDevice device = deviceRepository.findByDeviceSerial(deviceSerial).orElse(null);
        if (device == null || device.getBranchId() == null) {
            return List.of();
        }
        Map<Long, User> logins = userRepository.findByBranchId(device.getBranchId()).stream()
                .filter(user -> user.isActive() && !user.isDeleted() && LINKABLE_ROLES.contains(user.getRole()))
                .collect(Collectors.toMap(User::getId, user -> user));
        if (logins.isEmpty()) {
            return List.of();
        }
        List<FingerprintSyncResponse.StaffItem> items = new ArrayList<>();
        for (LoginFingerprint row : loginFingerprintRepository.findByUserIdIn(logins.keySet())) {
            User user = logins.get(row.getUserId());
            byte[] template = fingerprintCipher.decryptForLogin(row.getTemplateEncrypted(), row.getUserId());
            items.add(new FingerprintSyncResponse.StaffItem(
                    user.getId(), LOGIN_FINGER_PREFIX + user.getId(), user.getDisplayName(), user.getRole().name(),
                    Base64.getEncoder().encodeToString(template), sha256(template)));
        }
        return items;
    }

    // Same rules as an employee-linked finger: an active PC of the login's own branch, an active login of the right
    // role, and a sign-in finger registered for it. Audited the same way.
    private AuthResponse signInWithLoginFinger(String deviceSerial, String terminalUserId, String requestedRole) {
        Role wanted = null;
        if (!"STAFF".equals(requestedRole)) {
            try {
                wanted = Role.valueOf(requestedRole);
            } catch (IllegalArgumentException | NullPointerException e) {
                throw new InvalidOperationException("Unknown role");
            }
        }

        BiometricDevice device = deviceRepository.findByDeviceSerial(deviceSerial)
                .filter(found -> "ACTIVE".equals(found.getStatus()))
                .orElseThrow(() -> new InvalidOperationException("This PC's reader is not active"));

        Long userId;
        try {
            userId = Long.valueOf(terminalUserId.substring(LOGIN_FINGER_PREFIX.length()));
        } catch (NumberFormatException e) {
            throw new InvalidOperationException(notLinked(wanted));
        }
        final Role role = wanted;
        User user = userRepository.findById(userId)
                .filter(found -> found.isActive() && !found.isDeleted())
                .filter(found -> role == null ? LINKABLE_ROLES.contains(found.getRole()) : found.getRole() == role)
                .filter(found -> Objects.equals(found.getBranchId(), device.getBranchId()))
                .filter(found -> loginFingerprintRepository.existsByUserId(found.getId()))
                .orElseThrow(() -> new InvalidOperationException(
                        "This fingerprint can't sign in at this branch. Sign in with your password."));

        return authService.fingerprintLogin(user, deviceSerial);
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static String notLinked(Role role) {
        String who = role == null ? "Back Office or Branch Manager" : role == Role.BRANCH_MANAGER ? "Branch Manager" : "Back Office";
        return "This fingerprint is not linked to this branch's " + who + " login. "
                + "Ask the Admin to link the login to your employee record, or sign in with your password.";
    }
}
