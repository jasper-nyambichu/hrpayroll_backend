package com.company.hrpayroll.service;

import com.company.hrpayroll.config.BusinessClock;
import com.company.hrpayroll.dto.request.attendance.DeviceRegistrationRequest;
import com.company.hrpayroll.dto.request.attendance.EnrollmentRequest;
import com.company.hrpayroll.dto.request.attendance.FingerprintEnrollRequest;
import com.company.hrpayroll.entity.attendance.BiometricDevice;
import com.company.hrpayroll.entity.attendance.EmployeeFingerprint;
import com.company.hrpayroll.entity.auth.Role;
import com.company.hrpayroll.entity.auth.User;
import com.company.hrpayroll.entity.damage.Damage;
import com.company.hrpayroll.entity.employee.Employee;
import com.company.hrpayroll.exception.InvalidOperationException;
import com.company.hrpayroll.repository.attendance.AttendanceRepository;
import com.company.hrpayroll.repository.attendance.BiometricDeviceMappingRepository;
import com.company.hrpayroll.repository.attendance.BiometricDeviceRepository;
import com.company.hrpayroll.repository.attendance.EmployeeFingerprintRepository;
import com.company.hrpayroll.repository.branch.BranchRepository;
import com.company.hrpayroll.repository.damage.DamageRepository;
import com.company.hrpayroll.repository.employee.EmployeeRepository;
import com.company.hrpayroll.repository.leave.LeaveRequestRepository;
import com.company.hrpayroll.repository.settlement.AgentSettlementRepository;
import com.company.hrpayroll.repository.settlement.EmployeePayslipRepository;
import com.company.hrpayroll.security.AccessGuard;
import com.company.hrpayroll.service.attendance.impl.BiometricDeviceServiceImpl;
import com.company.hrpayroll.service.attendance.impl.BiometricEnrollmentServiceImpl;
import com.company.hrpayroll.service.attendance.impl.FingerprintCipher;
import com.company.hrpayroll.service.attendance.impl.TerminalIdentity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** A Back Office login works at its own branch only, and a fingerprint follows the employee, not the PC. */
class BackOfficeBranchScopeTest {

    private static final long OWN_BRANCH = 1L;
    private static final long OTHER_BRANCH = 2L;
    private static final long CARA = 30L;
    private static final String TERMINAL_ID = "emp-30";
    private static final String TEMPLATE = Base64.getEncoder().encodeToString(new byte[]{1, 2, 3, 4});

    private final BiometricDeviceRepository devices = mock(BiometricDeviceRepository.class);
    private final DamageRepository damages = mock(DamageRepository.class);
    private final EmployeeRepository employees = mock(EmployeeRepository.class);
    private final BiometricDeviceMappingRepository mappings = mock(BiometricDeviceMappingRepository.class);
    private final EmployeeFingerprintRepository fingerprints = mock(EmployeeFingerprintRepository.class);

    private final BranchRepository branches = mock(BranchRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final BiometricDeviceServiceImpl deviceService = new BiometricDeviceServiceImpl(devices, branches, encoder);

    private final AccessGuard guard = new AccessGuard(
            mock(AttendanceRepository.class), devices, damages, employees,
            mock(AgentSettlementRepository.class), mock(EmployeePayslipRepository.class), mock(LeaveRequestRepository.class));

    private final BiometricEnrollmentServiceImpl enrollment = new BiometricEnrollmentServiceImpl(
            mappings, fingerprints, new FingerprintCipher(Base64.getEncoder().encodeToString(new byte[32])),
            new TerminalIdentity(mappings, employees), devices, employees, mock(BranchRepository.class), mock(BusinessClock.class));

    @BeforeEach
    void signInAsBackOfficeOfOwnBranch() {
        User user = User.builder().username("bo").password("x").role(Role.BACK_OFFICE).branchId(OWN_BRANCH).build();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    @AfterEach
    void signOut() {
        SecurityContextHolder.clearContext();
    }

    private static BiometricDevice device(long id, long branchId, String serial) {
        BiometricDevice device = BiometricDevice.builder().branchId(branchId).deviceSerial(serial).build();
        device.setId(id);
        return device;
    }

    private Employee caraWorksAt(long branchId) {
        Employee employee = Employee.builder().fullName("Cara").employeeNumber("E-30").branchId(branchId).build();
        employee.setId(CARA);
        when(employees.findById(CARA)).thenReturn(Optional.of(employee));
        return employee;
    }

    @Test
    void anotherBranchsReaderIsNotMine() {
        when(devices.findById(5L)).thenReturn(Optional.of(device(5L, OWN_BRANCH, "A")));
        when(devices.findById(6L)).thenReturn(Optional.of(device(6L, OTHER_BRANCH, "B")));

        assertTrue(guard.isDeviceInMyBranch(5L));
        assertFalse(guard.isDeviceInMyBranch(6L));
        assertFalse(guard.isDeviceInMyBranch(7L));
    }

    @Test
    void aDamageIsOpenToItsBranchAndToWhereTheEmployeeWorksNow() {
        caraWorksAt(OWN_BRANCH);
        when(damages.findById(1L)).thenReturn(Optional.of(Damage.builder().employeeId(CARA).branchId(OTHER_BRANCH).build()));
        assertTrue(guard.isDamageInMyBranch(1L), "moved here with a damage from the old branch");

        caraWorksAt(OTHER_BRANCH);
        when(damages.findById(2L)).thenReturn(Optional.of(Damage.builder().employeeId(CARA).branchId(OWN_BRANCH).build()));
        assertTrue(guard.isDamageInMyBranch(2L), "recorded here before they moved away");

        when(damages.findById(3L)).thenReturn(Optional.of(Damage.builder().employeeId(CARA).branchId(OTHER_BRANCH).build()));
        assertFalse(guard.isDamageInMyBranch(3L), "another branch's business");
    }

    @Test
    void aBackOfficeWithNoBranchHasNoBranchAccess() {
        User user = User.builder().username("bo").password("x").role(Role.BACK_OFFICE).build();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
        caraWorksAt(OWN_BRANCH);

        assertFalse(guard.isSameBranch(OWN_BRANCH));
        assertFalse(guard.isEmployeeInMyBranch(CARA));
    }

    @Test
    void aBackOfficeCannotEnrolOnAnotherBranchsReader() {
        caraWorksAt(OTHER_BRANCH);
        when(devices.findByDeviceSerial("B")).thenReturn(Optional.of(device(6L, OTHER_BRANCH, "B")));

        assertThrows(AccessDeniedException.class,
                () -> enrollment.enrollMapping(new EnrollmentRequest(CARA, "B", TERMINAL_ID)));
        assertThrows(AccessDeniedException.class,
                () -> enrollment.saveFingerprint(new FingerprintEnrollRequest(CARA, "B", TERMINAL_ID, (short) 1, TEMPLATE, null)));
    }

    @Test
    void anOldPcBackingUpCannotReplaceTheCopyOfSomeoneWhoMovedAway() {
        caraWorksAt(OTHER_BRANCH);
        when(devices.findByDeviceSerial("A")).thenReturn(Optional.of(device(5L, OWN_BRANCH, "A")));
        when(mappings.findByTerminalUserId(TERMINAL_ID)).thenReturn(List.of());
        when(mappings.existsByDeviceIdAndTerminalUserId(5L, TERMINAL_ID)).thenReturn(true);
        when(fingerprints.findByEmployeeIdAndFingerSlot(CARA, (short) 1))
                .thenReturn(Optional.of(EmployeeFingerprint.builder().employeeId(CARA).fingerSlot((short) 1).build()));

        enrollment.saveFingerprint(new FingerprintEnrollRequest(CARA, "A", TERMINAL_ID, (short) 1, TEMPLATE, null));

        verify(fingerprints, never()).save(any());
    }

    @Test
    void registeringAReaderAgainForItsOwnBranchIssuesANewToken() {
        BiometricDevice reader = device(5L, OWN_BRANCH, "A");
        reader.setStatus("ACTIVE");
        reader.setDeviceTokenHash("old-hash");
        when(devices.findByDeviceSerial("A")).thenReturn(Optional.of(reader));
        when(branches.existsById(OWN_BRANCH)).thenReturn(true);
        when(encoder.encode(any())).thenReturn("new-hash");

        var response = deviceService.registerDevice(new DeviceRegistrationRequest(OWN_BRANCH, "A"));

        assertEquals(5L, response.id());
        assertEquals("new-hash", reader.getDeviceTokenHash());
        verify(devices).save(reader);
    }

    @Test
    void aReaderCannotBeTakenFromAnotherBranchOrReactivated() {
        when(branches.existsById(any())).thenReturn(true);

        BiometricDevice elsewhere = device(6L, OTHER_BRANCH, "B");
        elsewhere.setStatus("ACTIVE");
        when(devices.findByDeviceSerial("B")).thenReturn(Optional.of(elsewhere));
        assertThrows(InvalidOperationException.class,
                () -> deviceService.registerDevice(new DeviceRegistrationRequest(OWN_BRANCH, "B")));

        BiometricDevice deactivated = device(5L, OWN_BRANCH, "A");
        deactivated.setStatus("INACTIVE");
        when(devices.findByDeviceSerial("A")).thenReturn(Optional.of(deactivated));
        assertThrows(InvalidOperationException.class,
                () -> deviceService.registerDevice(new DeviceRegistrationRequest(OWN_BRANCH, "A")));

        verify(devices, never()).save(any());
    }

    @Test
    void anOldPcMayFillTheGapWhenTheCompanyHasNoCopyYet() {
        caraWorksAt(OTHER_BRANCH);
        when(devices.findByDeviceSerial("A")).thenReturn(Optional.of(device(5L, OWN_BRANCH, "A")));
        when(mappings.findByTerminalUserId(TERMINAL_ID)).thenReturn(List.of());
        when(mappings.existsByDeviceIdAndTerminalUserId(5L, TERMINAL_ID)).thenReturn(true);
        when(fingerprints.findByEmployeeIdAndFingerSlot(CARA, (short) 1)).thenReturn(Optional.empty());

        enrollment.saveFingerprint(new FingerprintEnrollRequest(CARA, "A", TERMINAL_ID, (short) 1, TEMPLATE, null));

        verify(fingerprints).save(any());
    }
}
