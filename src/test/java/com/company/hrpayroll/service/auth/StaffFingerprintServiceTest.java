package com.company.hrpayroll.service.auth;

import com.company.hrpayroll.dto.response.auth.AuthResponse;
import com.company.hrpayroll.entity.attendance.BiometricDevice;
import com.company.hrpayroll.entity.auth.Role;
import com.company.hrpayroll.entity.auth.User;
import com.company.hrpayroll.entity.employee.Employee;
import com.company.hrpayroll.exception.InvalidOperationException;
import com.company.hrpayroll.mapper.auth.UserMapper;
import com.company.hrpayroll.repository.attendance.BiometricDeviceRepository;
import com.company.hrpayroll.repository.auth.UserRepository;
import com.company.hrpayroll.repository.employee.EmployeeRepository;
import com.company.hrpayroll.service.attendance.impl.TerminalIdentity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StaffFingerprintServiceTest {

    private static final long BRANCH = 1L;
    private static final long OTHER_BRANCH = 2L;

    private final UserRepository users = mock(UserRepository.class);
    private final EmployeeRepository employees = mock(EmployeeRepository.class);
    private final BiometricDeviceRepository devices = mock(BiometricDeviceRepository.class);
    private final TerminalIdentity terminal = mock(TerminalIdentity.class);
    private final AuthService auth = mock(AuthService.class);
    private final StaffFingerprintService service =
            new StaffFingerprintService(users, employees, devices, terminal, new UserMapper(), auth);

    private final AuthResponse session = mock(AuthResponse.class);

    private static User login(long id, Role role, long branchId, Long employeeId) {
        User user = new User();
        user.setId(id);
        user.setUsername("user" + id);
        user.setRole(role);
        user.setBranchId(branchId);
        user.setEmployeeId(employeeId);
        user.setActive(true);
        return user;
    }

    private static Employee employee(long id, long branchId) {
        Employee employee = new Employee();
        employee.setId(id);
        employee.setBranchId(branchId);
        employee.setEmploymentStatus("ACTIVE");
        return employee;
    }

    @BeforeEach
    void pc() {
        BiometricDevice device = new BiometricDevice();
        device.setId(9L);
        device.setBranchId(BRANCH);
        device.setDeviceSerial("PC-1");
        device.setStatus("ACTIVE");
        when(devices.findByDeviceSerial("PC-1")).thenReturn(Optional.of(device));
        when(terminal.employeeFor(9L, "emp-5")).thenReturn(Optional.of(5L));
        when(auth.fingerprintLogin(any(), anyString())).thenReturn(session);
    }

    @Test
    void theLinkedManagerOfThisBranchIsSignedIn() {
        when(employees.findById(5L)).thenReturn(Optional.of(employee(5L, BRANCH)));
        User manager = login(20L, Role.BRANCH_MANAGER, BRANCH, 5L);
        when(users.findByEmployeeIdIn(List.of(5L))).thenReturn(List.of(manager));

        assertSame(session, service.signIn("PC-1", "emp-5", "BRANCH_MANAGER"));
        verify(auth).fingerprintLogin(manager, "PC-1");
    }

    @Test
    void anUnlinkedFingerOrTheWrongRoleOrBranchIsRefused() {
        when(employees.findById(5L)).thenReturn(Optional.of(employee(5L, BRANCH)));

        when(users.findByEmployeeIdIn(List.of(5L))).thenReturn(List.of());
        assertThrows(InvalidOperationException.class, () -> service.signIn("PC-1", "emp-5", "BRANCH_MANAGER"));

        // Linked, but as the Back Office: cannot open the manager's screen.
        when(users.findByEmployeeIdIn(List.of(5L))).thenReturn(List.of(login(21L, Role.BACK_OFFICE, BRANCH, 5L)));
        assertThrows(InvalidOperationException.class, () -> service.signIn("PC-1", "emp-5", "BRANCH_MANAGER"));

        // A manager of another branch, on this branch's PC.
        when(users.findByEmployeeIdIn(List.of(5L))).thenReturn(List.of(login(22L, Role.BRANCH_MANAGER, OTHER_BRANCH, 5L)));
        assertThrows(InvalidOperationException.class, () -> service.signIn("PC-1", "emp-5", "BRANCH_MANAGER"));

        // Never an Admin, whatever the finger.
        assertThrows(InvalidOperationException.class, () -> service.signIn("PC-1", "emp-5", "SUPER_ADMIN"));
        verify(auth, never()).fingerprintLogin(any(), anyString());
    }

    @Test
    void anEmployeeOfAnotherBranchOrAnUnknownFingerIsRefused() {
        when(employees.findById(5L)).thenReturn(Optional.of(employee(5L, OTHER_BRANCH)));
        assertThrows(InvalidOperationException.class, () -> service.signIn("PC-1", "emp-5", "BACK_OFFICE"));
        assertThrows(InvalidOperationException.class, () -> service.signIn("PC-1", "emp-77", "BACK_OFFICE"));
        verify(auth, never()).fingerprintLogin(any(), anyString());
    }

    @Test
    void linkingNeedsAnActiveEmployeeOfTheSameBranchNotLinkedElsewhere() {
        User manager = login(20L, Role.BRANCH_MANAGER, BRANCH, null);
        when(users.findById(20L)).thenReturn(Optional.of(manager));
        when(users.save(any())).thenAnswer(call -> call.getArgument(0));

        when(employees.findById(6L)).thenReturn(Optional.of(employee(6L, OTHER_BRANCH)));
        assertThrows(InvalidOperationException.class, () -> service.linkEmployee(20L, 6L));

        when(employees.findById(5L)).thenReturn(Optional.of(employee(5L, BRANCH)));
        when(users.findByEmployeeIdIn(List.of(5L))).thenReturn(List.of(login(21L, Role.BACK_OFFICE, BRANCH, 5L)));
        assertThrows(InvalidOperationException.class, () -> service.linkEmployee(20L, 5L));

        when(users.findByEmployeeIdIn(List.of(5L))).thenReturn(List.of());
        assertEquals(5L, service.linkEmployee(20L, 5L).employeeId());
        assertEquals(null, service.linkEmployee(20L, null).employeeId());
    }

    @Test
    void theRosterMarksOnlyActiveLinkedStaffOfTheBranch() {
        User inactive = login(23L, Role.BACK_OFFICE, BRANCH, 8L);
        inactive.setActive(false);
        when(users.findByBranchId(BRANCH)).thenReturn(List.of(
                login(20L, Role.BRANCH_MANAGER, BRANCH, 5L), inactive, login(24L, Role.BACK_OFFICE, BRANCH, null)));

        assertEquals(Map.of(5L, "BRANCH_MANAGER"), service.staffRolesForDevice("PC-1"));
    }
}
