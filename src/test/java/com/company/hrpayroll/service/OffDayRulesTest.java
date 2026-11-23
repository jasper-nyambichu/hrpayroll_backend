package com.company.hrpayroll.service;

import com.company.hrpayroll.config.BusinessClock;
import com.company.hrpayroll.dto.request.offday.OffDayRequest;
import com.company.hrpayroll.dto.request.offday.OffDayUpdateRequest;
import com.company.hrpayroll.entity.auth.Role;
import com.company.hrpayroll.entity.employee.Employee;
import com.company.hrpayroll.entity.offday.OffDay;
import com.company.hrpayroll.exception.InvalidOperationException;
import com.company.hrpayroll.repository.agent.AgentRepository;
import com.company.hrpayroll.repository.branch.BranchRepository;
import com.company.hrpayroll.repository.employee.EmployeeRepository;
import com.company.hrpayroll.repository.offday.OffDayRepository;
import com.company.hrpayroll.service.offday.OffDayService.Actor;
import com.company.hrpayroll.service.offday.impl.OffDayServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Off days are two single days a month, given by the agent, the Branch Manager or the Admin. */
class OffDayRulesTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 2);

    private final OffDayRepository offDays = mock(OffDayRepository.class);
    private final EmployeeRepository employees = mock(EmployeeRepository.class);
    private final BusinessClock clock = mock(BusinessClock.class);
    private final OffDayServiceImpl service = new OffDayServiceImpl(offDays, employees,
            mock(BranchRepository.class), mock(AgentRepository.class), clock,
            mock(com.company.hrpayroll.service.company.OrganisationOperations.class),
            mock(com.company.hrpayroll.repository.department.DepartmentRepository.class));

    private final List<OffDay> stored = new ArrayList<>();

    private static final Actor AGENT = new Actor(Role.AGENT, 7L, null);
    private static final Actor OTHER_AGENT = new Actor(Role.AGENT, 8L, null);
    private static final Actor MANAGER = new Actor(Role.BRANCH_MANAGER, null, 1L);
    private static final Actor OTHER_MANAGER = new Actor(Role.BRANCH_MANAGER, null, 2L);
    private static final Actor ADMIN = new Actor(Role.ADMIN, null, null);

    @BeforeEach
    void setUp() {
        Employee employee = new Employee();
        employee.setId(5L);
        employee.setFullName("Jane Wanjiru");
        employee.setBranchId(1L);
        employee.setAgentId(7L);
        employee.setEmploymentStatus("ACTIVE");
        when(employees.findById(5L)).thenReturn(Optional.of(employee));
        when(clock.today()).thenReturn(TODAY);

        long[] ids = {100};
        when(offDays.save(any())).thenAnswer(call -> {
            OffDay row = call.getArgument(0);
            if (row.getId() == null) { row.setId(ids[0]++); stored.add(row); }
            return row;
        });
        when(offDays.findById(anyLong())).thenAnswer(call -> stored.stream().filter(r -> r.getId().equals(call.getArgument(0))).findFirst());
        when(offDays.findByEmployeeIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(any(), any(), any())).thenAnswer(call -> {
            LocalDate end = call.getArgument(1), start = call.getArgument(2);
            return stored.stream().filter(r -> !r.getStartDate().isAfter(end) && !r.getEndDate().isBefore(start)).toList();
        });
        when(offDays.existsByEmployeeIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(any(), any(), any())).thenAnswer(call -> {
            LocalDate end = call.getArgument(1), start = call.getArgument(2);
            return stored.stream().anyMatch(r -> !r.getStartDate().isAfter(end) && !r.getEndDate().isBefore(start));
        });
    }

    private static OffDayRequest days(LocalDate... dates) {
        return new OffDayRequest(5L, List.of(dates), null);
    }

    @Test
    void twoSeparateDaysAreSavedAsTwoSingleDays() {
        var saved = service.grant(AGENT, days(LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 16)));
        assertEquals(2, saved.size());
        assertEquals(saved.get(0).startDate(), saved.get(0).endDate());
        assertEquals(LocalDate.of(2026, 11, 16), saved.get(1).startDate());
    }

    @Test
    void aThirdDayInTheSameMonthIsRefusedButNextMonthIsFine() {
        service.grant(AGENT, days(LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 4)));
        assertThrows(InvalidOperationException.class, () -> service.grant(AGENT, days(LocalDate.of(2026, 11, 20))));
        assertEquals(1, service.grant(AGENT, days(LocalDate.of(2026, 12, 1))).size());
    }

    @Test
    void theSameDayTwiceIsRefused() {
        assertThrows(InvalidOperationException.class, () -> service.grant(AGENT, days(LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 3))));
        service.grant(AGENT, days(LocalDate.of(2026, 11, 3)));
        assertThrows(InvalidOperationException.class, () -> service.grant(AGENT, days(LocalDate.of(2026, 11, 3))));
    }

    @Test
    void theBranchManagerAndTheAdminMayGiveDaysOffButNotOtherBranchesOrAgents() {
        assertEquals(1, service.grant(MANAGER, days(LocalDate.of(2026, 11, 3))).size());
        assertEquals(1, service.grant(ADMIN, days(LocalDate.of(2026, 11, 16))).size());
        assertThrows(InvalidOperationException.class, () -> service.grant(OTHER_MANAGER, days(LocalDate.of(2026, 12, 3))));
        assertThrows(InvalidOperationException.class, () -> service.grant(OTHER_AGENT, days(LocalDate.of(2026, 12, 3))));
    }

    @Test
    void anOffDayCanBeMovedWithinTheLimitButNotIntoAFullMonthOrThePast() {
        var saved = service.grant(AGENT, days(LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 16)));
        Long first = saved.get(0).id();

        var moved = service.update(MANAGER, first, new OffDayUpdateRequest(LocalDate.of(2026, 11, 10), "swap"));
        assertEquals(LocalDate.of(2026, 11, 10), moved.startDate());
        assertEquals("swap", moved.reason());

        assertThrows(InvalidOperationException.class, () -> service.update(ADMIN, first, new OffDayUpdateRequest(LocalDate.of(2026, 11, 16), null)));
        assertThrows(InvalidOperationException.class, () -> service.update(ADMIN, first, new OffDayUpdateRequest(LocalDate.of(2026, 11, 1), null)));

        service.grant(AGENT, days(LocalDate.of(2026, 12, 5), LocalDate.of(2026, 12, 6)));
        assertThrows(InvalidOperationException.class, () -> service.update(ADMIN, first, new OffDayUpdateRequest(LocalDate.of(2026, 12, 9), null)));
    }
}
