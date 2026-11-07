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
        Role role;
        try {
            role = Role.valueOf(requestedRole);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new InvalidOperationException("Unknown role");
        }
        if (!LINKABLE_ROLES.contains(role)) {
            throw new InvalidOperationException("Fingerprint sign-in is for the Branch Manager and the Back Office only");
        }

        BiometricDevice device = deviceRepository.findByDeviceSerial(deviceSerial)
                .filter(found -> "ACTIVE".equals(found.getStatus()))
                .orElseThrow(() -> new InvalidOperationException("This PC's reader is not active"));

        Long employeeId = terminalIdentity.employeeFor(device.getId(), terminalUserId).orElse(null);
        Employee employee = employeeId == null ? null : employeeRepository.findById(employeeId).orElse(null);
        if (employee == null || !Objects.equals(employee.getBranchId(), device.getBranchId())) {
            throw new InvalidOperationException(notLinked(role));
        }

        User user = userRepository.findByEmployeeIdIn(List.of(employeeId)).stream()
                .filter(found -> found.getRole() == role && found.isActive() && !found.isDeleted())
                .filter(found -> Objects.equals(found.getBranchId(), device.getBranchId()))
                .findFirst()
                .orElseThrow(() -> new InvalidOperationException(notLinked(role)));

        return authService.fingerprintLogin(user, deviceSerial);
    }

    private static String notLinked(Role role) {
        String who = role == Role.BRANCH_MANAGER ? "Branch Manager" : "Back Office";
        return "This fingerprint is not linked to this branch's " + who + " login. "
                + "Ask the Admin to link the login to your employee record, or sign in with your password.";
    }
}
