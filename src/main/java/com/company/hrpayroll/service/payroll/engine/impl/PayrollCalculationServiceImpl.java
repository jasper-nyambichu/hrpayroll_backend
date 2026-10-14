// service/payroll/engine/impl/PayrollCalculationServiceImpl.java
package com.company.hrpayroll.service.payroll.engine.impl;

import com.company.hrpayroll.entity.attendance.Attendance;
import com.company.hrpayroll.entity.company.CompanySettings;
import com.company.hrpayroll.entity.employee.Employee;
import com.company.hrpayroll.entity.leave.LeaveRequest;
import com.company.hrpayroll.entity.leave.LeaveStatus;
import com.company.hrpayroll.entity.leave.LeaveType;
import com.company.hrpayroll.entity.payroll.PayrollRun;
import com.company.hrpayroll.entity.payroll.Payslip;
import com.company.hrpayroll.exception.ResourceNotFoundException;
import com.company.hrpayroll.repository.attendance.AttendanceRepository;
import com.company.hrpayroll.repository.company.CompanySettingsRepository;
import com.company.hrpayroll.repository.leave.LeaveRequestRepository;
import com.company.hrpayroll.repository.leave.LeaveTypeRepository;
import com.company.hrpayroll.service.loan.LoanService;
import com.company.hrpayroll.service.payroll.engine.PayrollCalculationService;
import com.company.hrpayroll.service.payroll.engine.StatutoryDeductionCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PayrollCalculationServiceImpl implements PayrollCalculationService {

    private final AttendanceRepository attendanceRepository;
    private final CompanySettingsRepository companySettingsRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final StatutoryDeductionCalculator statutoryDeductionCalculator;
    private final LoanService loanService;

    @Override
    public Payslip calculateForEmployee(Employee employee, PayrollRun run) {
        CompanySettings settings = currentSettings();

        BigDecimal overtimePay = calculateOvertimePay(employee, run, settings);
        BigDecimal unpaidLeaveDeduction = calculateUnpaidLeaveDeduction(employee, run, settings);

        // House/transport/bonus/other allowances are not yet modeled as
        // recurring per-employee amounts (Employee only stores basicSalary
        // today). They default to zero here — once an EmployeeAllowance
        // structure exists, this is the one method that needs to change.
        BigDecimal houseAllowance = BigDecimal.ZERO;
        BigDecimal transportAllowance = BigDecimal.ZERO;
        BigDecimal bonus = BigDecimal.ZERO;
        BigDecimal otherAllowances = BigDecimal.ZERO;

        // Unpaid leave reduces what the employee actually earned, so it comes off
        // BEFORE gross — SHIF, NSSF, the levy and PAYE are all worked on real pay.
        BigDecimal grossPay = employee.getBasicSalary()
                .subtract(unpaidLeaveDeduction)
                .add(houseAllowance)
                .add(transportAllowance)
                .add(overtimePay)
                .add(bonus)
                .add(otherAllowances);

        BigDecimal shif = statutoryDeductionCalculator.calculateShif(grossPay);
        BigDecimal nssf = statutoryDeductionCalculator.calculateNssf(grossPay);
        BigDecimal housingLevy = statutoryDeductionCalculator.calculateHousingLevy(grossPay);
        BigDecimal nita = statutoryDeductionCalculator.calculateNita();
        BigDecimal paye = statutoryDeductionCalculator.calculatePaye(grossPay, nssf, shif, housingLevy);

        BigDecimal loanDeduction = loanService.getActiveDeductionsForEmployees(List.of(employee.getId())).stream()
                .map(LoanService.LoanDeductionResult::installmentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal otherDeductions = BigDecimal.ZERO;

        // NITA is an EMPLOYER levy: it is recorded on the payslip (nitaDeduction)
        // for remittance, but it is NOT taken out of the employee's pay.
        BigDecimal totalDeductions = shif.add(nssf).add(housingLevy).add(paye)
                .add(loanDeduction).add(otherDeductions);

        BigDecimal netPay = grossPay.subtract(totalDeductions).setScale(2, RoundingMode.HALF_UP);

        return Payslip.builder()
                .payrollRunId(run.getId())
                .employeeId(employee.getId())
                .basicSalary(employee.getBasicSalary())
                .houseAllowance(houseAllowance)
                .transportAllowance(transportAllowance)
                .overtimePay(overtimePay)
                .bonus(bonus)
                .otherAllowances(otherAllowances)
                .unpaidLeaveDeduction(unpaidLeaveDeduction)
                .grossPay(grossPay)
                .shifDeduction(shif)
                .nssfDeduction(nssf)
                .housingLevyDeduction(housingLevy)
                .payeDeduction(paye)
                .nitaDeduction(nita)
                .loanDeduction(loanDeduction)
                .otherDeductions(otherDeductions)
                .netPay(netPay)
                .build();
    }

    @Override
    public List<Payslip> calculateForEmployees(List<Employee> employees, PayrollRun run) {
        return employees.stream()
                .map(employee -> calculateForEmployee(employee, run))
                .toList();
    }

    // Overtime pay = hours worked under an OVERTIME-status attendance record,
    // valued at (basic salary / standard hours in the period) x the
    // overtime multiplier from company settings.
    private BigDecimal calculateOvertimePay(Employee employee, PayrollRun run, CompanySettings settings) {
        List<Attendance> overtimeRecords = attendanceRepository
                .findByEmployeeIdAndAttendanceDateBetween(employee.getId(), run.getPeriodStart(), run.getPeriodEnd())
                .stream()
                .filter(a -> "OVERTIME".equals(a.getStatus()) && a.getClockIn() != null && a.getClockOut() != null)
                .toList();

        if (overtimeRecords.isEmpty()) {
            return BigDecimal.ZERO;
        }

        double totalOvertimeHours = overtimeRecords.stream()
                .mapToDouble(a -> Duration.between(a.getClockIn(), a.getClockOut()).toMinutes() / 60.0)
                .sum();

        int workingDaysInPeriod = countWorkingDays(run.getPeriodStart(), run.getPeriodEnd(), settings.getWeekendDays());
        if (workingDaysInPeriod == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalStandardHours = BigDecimal.valueOf(settings.getDefaultWorkingHoursPerDay())
                .multiply(BigDecimal.valueOf(workingDaysInPeriod));

        if (totalStandardHours.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal hourlyRate = employee.getBasicSalary().divide(totalStandardHours, 4, RoundingMode.HALF_UP);
        BigDecimal overtimeMultiplier = BigDecimal.valueOf(settings.getOvertimeMultiplier());

        return hourlyRate.multiply(BigDecimal.valueOf(totalOvertimeHours))
                .multiply(overtimeMultiplier)
                .setScale(2, RoundingMode.HALF_UP);
    }

    // Approved leave of a type with paid = false (e.g. UNPAID) that falls inside
    // the period is not paid. Each unpaid WORKING day costs
    // basic / working days in the period; the run is assumed to cover one pay
    // period (company_settings.payrollPeriod = MONTHLY). Days are collected in a
    // set so two overlapping leave requests never deduct the same day twice, and
    // the deduction can never exceed the basic salary.
    private BigDecimal calculateUnpaidLeaveDeduction(Employee employee, PayrollRun run, CompanySettings settings) {
        int workingDaysInPeriod = countWorkingDays(run.getPeriodStart(), run.getPeriodEnd(), settings.getWeekendDays());
        if (workingDaysInPeriod == 0) {
            return BigDecimal.ZERO;
        }

        Set<DayOfWeek> weekendDays = parseWeekendDays(settings.getWeekendDays());
        Set<LocalDate> unpaidDays = new HashSet<>();

        List<LeaveRequest> approvedLeave = leaveRequestRepository
                .findByEmployeeIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        employee.getId(), LeaveStatus.APPROVED, run.getPeriodEnd(), run.getPeriodStart());

        for (LeaveRequest leave : approvedLeave) {
            boolean paid = leaveTypeRepository.findById(leave.getLeaveTypeId())
                    .map(LeaveType::isPaid)
                    .orElse(true);
            if (paid) {
                continue;
            }

            LocalDate from = leave.getStartDate().isAfter(run.getPeriodStart()) ? leave.getStartDate() : run.getPeriodStart();
            LocalDate to = leave.getEndDate().isBefore(run.getPeriodEnd()) ? leave.getEndDate() : run.getPeriodEnd();

            for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
                if (!weekendDays.contains(date.getDayOfWeek())) {
                    unpaidDays.add(date);
                }
            }
        }

        if (unpaidDays.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal deduction = employee.getBasicSalary()
                .multiply(BigDecimal.valueOf(unpaidDays.size()))
                .divide(BigDecimal.valueOf(workingDaysInPeriod), 2, RoundingMode.HALF_UP);

        return deduction.min(employee.getBasicSalary());
    }

    // Uses CompanySettings.weekendDays (e.g. "SATURDAY,SUNDAY") so working-day
    // math respects whatever the company has actually configured, rather
    // than assuming a fixed number of working days per month.
    private int countWorkingDays(LocalDate start, LocalDate end, String weekendDaysCsv) {
        Set<DayOfWeek> weekendDays = parseWeekendDays(weekendDaysCsv);

        int count = 0;
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            if (!weekendDays.contains(date.getDayOfWeek())) {
                count++;
            }
        }
        return count;
    }

    private Set<DayOfWeek> parseWeekendDays(String weekendDaysCsv) {
        return Arrays.stream(weekendDaysCsv.split(","))
                .map(String::trim)
                .map(String::toUpperCase)
                .map(DayOfWeek::valueOf)
                .collect(Collectors.toSet());
    }

    private CompanySettings currentSettings() {
        return companySettingsRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Company settings have not been configured yet"));
    }
}