package com.company.hrpayroll.service;

import com.company.hrpayroll.config.BusinessClock;
import com.company.hrpayroll.dto.request.dayswork.DaysWorkedLockRequest;
import com.company.hrpayroll.entity.attendance.Attendance;
import com.company.hrpayroll.entity.auth.Role;
import com.company.hrpayroll.entity.auth.User;
import com.company.hrpayroll.entity.branch.Branch;
import com.company.hrpayroll.entity.dayswork.DaysWorkedRecord;
import com.company.hrpayroll.entity.employee.Employee;
import com.company.hrpayroll.exception.DuplicateResourceException;
import com.company.hrpayroll.exception.InvalidOperationException;
import com.company.hrpayroll.mapper.auth.UserMapper;
import com.company.hrpayroll.mapper.dayswork.DaysWorkedMapper;
import com.company.hrpayroll.mapper.employee.EmployeeMapper;
import com.company.hrpayroll.repository.agent.AgentRepository;
import com.company.hrpayroll.repository.attendance.AttendanceRepository;
import com.company.hrpayroll.repository.attendance.BiometricDeviceMappingRepository;
import com.company.hrpayroll.repository.attendance.EmployeeFingerprintRepository;
import com.company.hrpayroll.repository.auth.UserRepository;
import com.company.hrpayroll.repository.branch.BranchRepository;
import com.company.hrpayroll.repository.damage.DamageRepository;
import com.company.hrpayroll.repository.dayswork.DaysWorkedRecordRepository;
import com.company.hrpayroll.repository.department.DepartmentRepository;
import com.company.hrpayroll.repository.employee.EmployeeRepository;
import com.company.hrpayroll.repository.loan.LoanRepository;
import com.company.hrpayroll.repository.settlement.AgentSettlementRepository;
import com.company.hrpayroll.service.auth.impl.UserServiceImpl;
import com.company.hrpayroll.service.dayswork.impl.DaysWorkedServiceImpl;
import com.company.hrpayroll.service.employee.impl.EmployeeServiceImpl;
import com.company.hrpayroll.service.notification.NotificationService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** What moving someone to another branch must do, and what it must refuse. */
class BranchMoveTest {

    private static final Long OLD_BRANCH = 1L;
    private static final Long NEW_BRANCH = 2L;

    private static Branch branch(Long id, String name, String status, Long managerId) {
        Branch branch = new Branch();
        branch.setId(id);
        branch.setName(name);
        branch.setStatus(status);
        branch.setManagerId(managerId);
        return branch;
    }

    private static User manager(Long id, Long branchId, String displayName) {
        User user = User.builder()
                .username("manager" + id)
                .role(Role.BRANCH_MANAGER)
                .displayName(displayName)
                .branchId(branchId)
                .active(true)
                .build();
        user.setId(id);
        return user;
    }

    private static Employee employee(Long id, Long branchId) {
        Employee employee = new Employee();
        employee.setId(id);
        employee.setFullName("Employee " + id);
        employee.setBranchId(branchId);
        employee.setEmploymentStatus("ACTIVE");
        return employee;
    }

    /* ------------------------------------------------------------ Branch Manager */

    private final UserRepository users = mock(UserRepository.class);
    private final BranchRepository branches = mock(BranchRepository.class);
    private final NotificationService notifications = mock(NotificationService.class);
    private final UserServiceImpl userService = new UserServiceImpl(
            users, branches, mock(AgentRepository.class), new UserMapper(), mock(PasswordEncoder.class), notifications,
            mock(com.company.hrpayroll.service.auth.impl.IssuedPasswordCipher.class), mock(com.company.hrpayroll.config.BusinessClock.class));

    private User managerAtOldBranch(Branch oldBranch, Branch newBranch) {
        User user = manager(10L, OLD_BRANCH, "Westlands Branch Manager");
        when(users.findById(10L)).thenReturn(Optional.of(user));
        when(users.save(any(User.class))).thenAnswer(call -> call.getArgument(0));
        when(branches.findById(OLD_BRANCH)).thenReturn(Optional.of(oldBranch));
        when(branches.findById(NEW_BRANCH)).thenReturn(Optional.of(newBranch));
        when(branches.findByManagerId(10L)).thenReturn(List.of(oldBranch));
        when(users.findByBranchId(NEW_BRANCH)).thenReturn(List.of());
        return user;
    }

    @Test
    void movingAManagerUpdatesTheLoginAndBothBranches() {
        Branch oldBranch = branch(OLD_BRANCH, "Westlands", "ACTIVE", 10L);
        Branch newBranch = branch(NEW_BRANCH, "Thika", "ACTIVE", null);
        User user = managerAtOldBranch(oldBranch, newBranch);

        userService.assignBranch(10L, NEW_BRANCH, false);

        assertEquals(NEW_BRANCH, user.getBranchId());
        assertNull(oldBranch.getManagerId());
        assertEquals(10L, newBranch.getManagerId());
        assertEquals("Thika Branch Manager", user.getDisplayName());
        verify(notifications).notifyInApp(eq(10L), eq("BRANCH_CHANGED"), any(), eq(NEW_BRANCH));
    }

    @Test
    void aNameTheAdminChoseIsKept() {
        User user = managerAtOldBranch(branch(OLD_BRANCH, "Westlands", "ACTIVE", 10L), branch(NEW_BRANCH, "Thika", "ACTIVE", null));
        user.setDisplayName("Grace");

        userService.assignBranch(10L, NEW_BRANCH, false);

        assertEquals("Grace", user.getDisplayName());
    }

    @Test
    void aBranchThatAlreadyHasAManagerIsRefusedAndNothingChanges() {
        Branch oldBranch = branch(OLD_BRANCH, "Westlands", "ACTIVE", 10L);
        Branch newBranch = branch(NEW_BRANCH, "Thika", "ACTIVE", 11L);
        User user = managerAtOldBranch(oldBranch, newBranch);
        when(users.findByBranchId(NEW_BRANCH)).thenReturn(List.of(manager(11L, NEW_BRANCH, "Thika Branch Manager")));

        assertThrows(DuplicateResourceException.class, () -> userService.assignBranch(10L, NEW_BRANCH, false));

        assertEquals(OLD_BRANCH, user.getBranchId());
        assertEquals(10L, oldBranch.getManagerId());
        assertEquals(11L, newBranch.getManagerId());
        verify(users, never()).save(any());
    }

    @Test
    void askingToSwapTradesTheTwoManagers() {
        Branch oldBranch = branch(OLD_BRANCH, "Westlands", "ACTIVE", 10L);
        Branch newBranch = branch(NEW_BRANCH, "Thika", "ACTIVE", 11L);
        User user = managerAtOldBranch(oldBranch, newBranch);
        User other = manager(11L, NEW_BRANCH, "Thika Branch Manager");
        when(users.findByBranchId(NEW_BRANCH)).thenReturn(List.of(other));

        userService.assignBranch(10L, NEW_BRANCH, true);

        assertEquals(NEW_BRANCH, user.getBranchId());
        assertEquals(OLD_BRANCH, other.getBranchId());
        assertEquals(10L, newBranch.getManagerId());
        assertEquals(11L, oldBranch.getManagerId());
        assertEquals("Thika Branch Manager", user.getDisplayName());
        assertEquals("Westlands Branch Manager", other.getDisplayName());
        verify(notifications).notifyInApp(eq(11L), eq("BRANCH_CHANGED"), any(), eq(OLD_BRANCH));
    }

    @Test
    void aBranchHoldingTwoManagersFromOlderDataCannotBeSwappedInto() {
        Branch newBranch = branch(NEW_BRANCH, "Thika", "ACTIVE", 11L);
        User user = managerAtOldBranch(branch(OLD_BRANCH, "Westlands", "ACTIVE", 10L), newBranch);
        when(users.findByBranchId(NEW_BRANCH)).thenReturn(List.of(manager(11L, NEW_BRANCH, "A"), manager(12L, NEW_BRANCH, "B")));

        assertThrows(DuplicateResourceException.class, () -> userService.assignBranch(10L, NEW_BRANCH, true));
        assertEquals(OLD_BRANCH, user.getBranchId());
    }

    @Test
    void anInactiveBranchOrTheSameBranchIsRefused() {
        managerAtOldBranch(branch(OLD_BRANCH, "Westlands", "ACTIVE", 10L), branch(NEW_BRANCH, "Thika", "INACTIVE", null));

        assertThrows(InvalidOperationException.class, () -> userService.assignBranch(10L, NEW_BRANCH, false));
        assertThrows(InvalidOperationException.class, () -> userService.assignBranch(10L, OLD_BRANCH, false));
    }

    @Test
    void anAgentLoginCannotBeMoved() {
        User agent = User.builder().username("agent").role(Role.AGENT).active(true).build();
        agent.setId(20L);
        when(users.findById(20L)).thenReturn(Optional.of(agent));

        assertThrows(InvalidOperationException.class, () -> userService.assignBranch(20L, NEW_BRANCH, false));
    }

    /* ------------------------------------------------------------ employee */

    private final EmployeeRepository employees = mock(EmployeeRepository.class);
    private final AttendanceRepository attendance = mock(AttendanceRepository.class);
    private final BiometricDeviceMappingRepository mappings = mock(BiometricDeviceMappingRepository.class);
    private final BusinessClock clock = mock(BusinessClock.class);
    private final DaysWorkedRecordRepository daysWorked = mock(DaysWorkedRecordRepository.class);
    private final EmployeeServiceImpl employeeService = new EmployeeServiceImpl(
            employees, branches, mock(DepartmentRepository.class), mock(AgentRepository.class), mock(EmployeeMapper.class),
            mappings, mock(EmployeeFingerprintRepository.class), mock(LoanRepository.class), mock(DamageRepository.class), daysWorked, attendance,
            mock(AgentSettlementRepository.class), users, clock);

    private Employee employeeAtOldBranch() {
        Employee employee = employee(5L, OLD_BRANCH);
        when(employees.findById(5L)).thenReturn(Optional.of(employee));
        when(employees.save(any(Employee.class))).thenAnswer(call -> call.getArgument(0));
        when(branches.findById(NEW_BRANCH)).thenReturn(Optional.of(branch(NEW_BRANCH, "Thika", "ACTIVE", null)));
        when(branches.findById(OLD_BRANCH)).thenReturn(Optional.of(branch(OLD_BRANCH, "Westlands", "ACTIVE", null)));
        when(clock.now()).thenReturn(LocalDateTime.of(2026, 11, 4, 12, 0));
        when(attendance.findFirstByEmployeeIdAndClockOutIsNullOrderByClockInDesc(5L)).thenReturn(Optional.empty());
        when(users.findByEmployeeId(5L)).thenReturn(Optional.empty());
        return employee;
    }

    private static Attendance openSince(LocalDateTime clockIn) {
        Attendance open = new Attendance();
        open.setClockIn(clockIn);
        return open;
    }

    @Test
    void movingAnEmployeeKeepsTheirEnrolmentAndMovesTheirLogin() {
        Employee employee = employeeAtOldBranch();
        User login = User.builder().username("e").role(Role.EMPLOYEE).branchId(OLD_BRANCH).build();
        when(users.findByEmployeeId(5L)).thenReturn(Optional.of(login));

        employeeService.assignBranch(5L, NEW_BRANCH);

        assertEquals(NEW_BRANCH, employee.getBranchId());
        assertEquals(NEW_BRANCH, login.getBranchId());
        verify(mappings, never()).deleteByEmployeeId(any());
    }

    @Test
    void movingAnEmployeeTakesTheirAttendanceHistoryWithThem() {
        employeeAtOldBranch();

        employeeService.assignBranch(5L, NEW_BRANCH);

        verify(attendance).moveEmployeeHistory(5L, NEW_BRANCH);
    }

    @Test
    void aRefusedMoveLeavesTheirAttendanceWhereItIs() {
        employeeAtOldBranch();
        when(attendance.findFirstByEmployeeIdAndClockOutIsNullOrderByClockInDesc(5L))
                .thenReturn(Optional.of(openSince(LocalDateTime.of(2026, 11, 4, 9, 0))));

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> employeeService.assignBranch(5L, NEW_BRANCH));

        verify(attendance, never()).moveEmployeeHistory(any(), any());
    }

    @Test
    void someoneClockedInCannotBeMovedAndNothingChanges() {
        Employee employee = employeeAtOldBranch();
        when(attendance.findFirstByEmployeeIdAndClockOutIsNullOrderByClockInDesc(5L))
                .thenReturn(Optional.of(openSince(LocalDateTime.of(2026, 11, 4, 8, 0))));

        InvalidOperationException refusal =
                assertThrows(InvalidOperationException.class, () -> employeeService.assignBranch(5L, NEW_BRANCH));

        assertTrue(refusal.getMessage().contains("clocked in"));
        assertEquals(OLD_BRANCH, employee.getBranchId());
        verify(employees, never()).save(any());
    }

    @Test
    void aClockOutForgottenLongAgoDoesNotBlockTheMove() {
        Employee employee = employeeAtOldBranch();
        when(attendance.findFirstByEmployeeIdAndClockOutIsNullOrderByClockInDesc(5L))
                .thenReturn(Optional.of(openSince(LocalDateTime.of(2026, 10, 20, 8, 0))));

        employeeService.assignBranch(5L, NEW_BRANCH);

        assertEquals(NEW_BRANCH, employee.getBranchId());
    }

    @Test
    void aTerminatedEmployeeOrTheSameBranchIsRefused() {
        Employee employee = employeeAtOldBranch();

        assertThrows(InvalidOperationException.class, () -> employeeService.assignBranch(5L, OLD_BRANCH));

        employee.setEmploymentStatus("TERMINATED");
        assertThrows(InvalidOperationException.class, () -> employeeService.assignBranch(5L, NEW_BRANCH));
    }

    /* ------------------------------------------------------------ days worked after a move */

    private static final LocalDate START = LocalDate.of(2026, 10, 1);
    private static final LocalDate END = LocalDate.of(2026, 10, 31);

    private final DaysWorkedServiceImpl daysWorkedService = new DaysWorkedServiceImpl(
            daysWorked, attendance, employees, mock(AgentSettlementRepository.class), mock(DaysWorkedMapper.class), clock);

    private static DaysWorkedRecord lockedAt(Long employeeId, Long branchId, LocalDate start, LocalDate end) {
        return DaysWorkedRecord.builder().employeeId(employeeId).branchId(branchId).periodStart(start).periodEnd(end).build();
    }

    @SuppressWarnings("unchecked")
    @Test
    void someoneLockedAtTheirOldBranchDoesNotStopTheNewBranchLocking() {
        when(employees.findByBranchId(NEW_BRANCH)).thenReturn(List.of(employee(5L, NEW_BRANCH), employee(6L, NEW_BRANCH)));
        when(daysWorked.findByEmployeeIdInAndPeriodStartLessThanEqualAndPeriodEndGreaterThanEqual(anyList(), eq(END), eq(START)))
                .thenReturn(List.of(lockedAt(5L, OLD_BRANCH, START, END)));
        when(attendance.findByEmployeeIdInAndAttendanceDateBetween(anyList(), eq(START), eq(END))).thenReturn(List.of());
        when(clock.now()).thenReturn(LocalDateTime.of(2026, 11, 4, 12, 0));
        when(daysWorked.saveAll(anyList())).thenAnswer(call -> call.getArgument(0));

        daysWorkedService.lockPeriod(new DaysWorkedLockRequest(NEW_BRANCH, START, END, null), 1L);

        ArgumentCaptor<List<DaysWorkedRecord>> saved = ArgumentCaptor.forClass(List.class);
        verify(daysWorked).saveAll(saved.capture());
        assertEquals(List.of(6L), saved.getValue().stream().map(DaysWorkedRecord::getEmployeeId).toList());
    }

    @Test
    void aPeriodLockedAtThisBranchOrPartlyLockedElsewhereIsStillRefused() {
        when(employees.findByBranchId(NEW_BRANCH)).thenReturn(List.of(employee(5L, NEW_BRANCH)));
        DaysWorkedLockRequest request = new DaysWorkedLockRequest(NEW_BRANCH, START, END, null);

        when(daysWorked.findByEmployeeIdInAndPeriodStartLessThanEqualAndPeriodEndGreaterThanEqual(anyList(), eq(END), eq(START)))
                .thenReturn(List.of(lockedAt(5L, NEW_BRANCH, START, END)));
        assertThrows(InvalidOperationException.class, () -> daysWorkedService.lockPeriod(request, 1L));

        when(daysWorked.findByEmployeeIdInAndPeriodStartLessThanEqualAndPeriodEndGreaterThanEqual(anyList(), eq(END), eq(START)))
                .thenReturn(List.of(lockedAt(5L, OLD_BRANCH, START, START.plusDays(14))));
        assertThrows(InvalidOperationException.class, () -> daysWorkedService.lockPeriod(request, 1L));
    }
}
