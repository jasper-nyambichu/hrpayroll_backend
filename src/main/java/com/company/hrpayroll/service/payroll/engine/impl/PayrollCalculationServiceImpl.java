// service/payroll/engine/impl/PayrollCalculationServiceImpl.java
package com.company.hrpayroll.service.payroll.engine.impl;

import com.company.hrpayroll.entity.attendance.Attendance;
import com.company.hrpayroll.entity.company.CompanySettings;
import com.company.hrpayroll.entity.employee.Employee;
import com.company.hrpayroll.entity.payroll.PayrollRun;
import com.company.hrpayroll.entity.payroll.Payslip;
import com.company.hrpayroll.exception.InvalidOperationException;
import com.company.hrpayroll.exception.ResourceNotFoundException;
import com.company.hrpayroll.repository.attendance.AttendanceRepository;
import com.company.hrpayroll.repository.company.CompanySettingsRepository;
import com.company.hrpayroll.service.damage.DamageService;
import com.company.hrpayroll.service.dayswork.DaysWorkedService;
import com.company.hrpayroll.service.loan.LoanService;
import com.company.hrpayroll.service.payroll.engine.PayrollCalculationService;
import com.company.hrpayroll.service.payroll.engine.PayslipCalculation;
import com.company.hrpayroll.service.payroll.engine.StatutoryDeductionCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PayrollCalculationServiceImpl implements PayrollCalculationService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final AttendanceRepository attendanceRepository;
    private final CompanySettingsRepository companySettingsRepository;
    private final StatutoryDeductionCalculator statutoryDeductionCalculator;
    private final LoanService loanService;
    private final DamageService damageService;
    private final DaysWorkedService daysWorkedService;

    @Override
    public PayslipCalculation calculate(Employee employee, PayrollRun run, int daysWorked) {
        CompanySettings settings = currentSettings();

        // Pay for the days the Back Office locked: 30 days x 1,000 = 30,000.
        BigDecimal dailyRate = settings.getDailyRate();
        BigDecimal basePay = dailyRate.multiply(BigDecimal.valueOf(daysWorked)).setScale(2, RoundingMode.HALF_UP);

        BigDecimal overtimePay = calculateOvertimePay(employee, run, settings);

        // House/transport/bonus/other allowances are not yet modeled as recurring per-employee
        // amounts. They default to zero here — once an EmployeeAllowance structure exists, this is
        // the one method that needs to change.
        BigDecimal houseAllowance = BigDecimal.ZERO;
        BigDecimal transportAllowance = BigDecimal.ZERO;
        BigDecimal bonus = BigDecimal.ZERO;
        BigDecimal otherAllowances = BigDecimal.ZERO;

        // An unworked day is simply not paid, so there is no separate unpaid-leave deduction any more.
        BigDecimal unpaidLeaveDeduction = BigDecimal.ZERO;

        BigDecimal grossPay = basePay
                .add(houseAllowance)
                .add(transportAllowance)
                .add(overtimePay)
                .add(bonus)
                .add(otherAllowances);

        // SHIF and NSSF as the Admin set them (a percentage of pay, or a fixed amount). The owner's
        // formula ADDS them to the pay: 30,000 + SHIF + NSSF = total employee salary. They are not
        // taken out of what the employee receives.
        BigDecimal shif = statutoryDeductionCalculator.calculateConfigured(grossPay, settings.getShifMode(), settings.getShifValue());
        BigDecimal nssf = statutoryDeductionCalculator.calculateConfigured(grossPay, settings.getNssfMode(), settings.getNssfValue());
        BigDecimal totalEmployeeSalary = grossPay.add(shif).add(nssf);

        // The other statutory items are switches the Admin turns on; off, they are zero.
        BigDecimal housingLevy = settings.isApplyHousingLevy()
                ? statutoryDeductionCalculator.calculateHousingLevy(grossPay) : BigDecimal.ZERO;
        BigDecimal paye = settings.isApplyPaye()
                ? statutoryDeductionCalculator.calculatePaye(grossPay, nssf, shif, housingLevy) : BigDecimal.ZERO;
        // NITA is an EMPLOYER levy: recorded for remittance, never taken out of the employee's pay.
        BigDecimal nita = settings.isApplyNita() ? statutoryDeductionCalculator.calculateNita() : BigDecimal.ZERO;

        BigDecimal otherDeductions = BigDecimal.ZERO;

        // Deduction ceiling: tax and levy always come off; loans and damages share what is left of
        // the ceiling (a percentage of gross pay), loans first.
        BigDecimal mandatory = housingLevy.add(paye).add(otherDeductions);
        BigDecimal ceiling = grossPay.multiply(settings.getDeductionCeilingPercent())
                .divide(HUNDRED, 2, RoundingMode.HALF_UP);
        BigDecimal remaining = ceiling.subtract(mandatory).max(BigDecimal.ZERO);

        List<LoanService.LoanDeductionResult> appliedLoans = new ArrayList<>();
        BigDecimal loanDeduction = BigDecimal.ZERO;
        for (LoanService.LoanDeductionResult due : loanService.getActiveDeductionsForEmployees(List.of(employee.getId()))) {
            BigDecimal amount = due.installmentAmount().min(remaining);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            appliedLoans.add(new LoanService.LoanDeductionResult(due.loanId(), due.employeeId(), amount));
            loanDeduction = loanDeduction.add(amount);
            remaining = remaining.subtract(amount);
        }

        // Damages the employee agreed to (or that were overridden), oldest first. A damage is taken
        // whole or not at all: one that does not fit under the ceiling waits for the next run.
        List<Long> deductedDamageIds = new ArrayList<>();
        BigDecimal damageDeduction = BigDecimal.ZERO;
        List<DamageService.DamageDeductionResult> approvedDamages =
                damageService.getApprovedDeductionsForEmployees(List.of(employee.getId())).stream()
                        .sorted(Comparator.comparing(DamageService.DamageDeductionResult::damageId))
                        .toList();
        for (DamageService.DamageDeductionResult damage : approvedDamages) {
            if (damage.amount().compareTo(remaining) > 0) {
                continue;
            }
            deductedDamageIds.add(damage.damageId());
            damageDeduction = damageDeduction.add(damage.amount());
            remaining = remaining.subtract(damage.amount());
        }

        BigDecimal totalDeductions = housingLevy.add(paye)
                .add(loanDeduction).add(damageDeduction).add(otherDeductions);

        BigDecimal netPay = grossPay.subtract(totalDeductions).setScale(2, RoundingMode.HALF_UP);

        Payslip payslip = Payslip.builder()
                .payrollRunId(run.getId())
                .employeeId(employee.getId())
                .daysWorked(daysWorked)
                .dailyRate(dailyRate)
                .basicSalary(basePay)
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
                .damageDeduction(damageDeduction)
                .otherDeductions(otherDeductions)
                .totalEmployeeSalary(totalEmployeeSalary)
                .netPay(netPay)
                .build();

        return new PayslipCalculation(payslip, appliedLoans, deductedDamageIds);
    }

    @Override
    public Payslip calculateForEmployee(Employee employee, PayrollRun run) {
        Map<Long, Integer> locked = daysWorkedService.getLockedDays(
                List.of(employee.getId()), run.getPeriodStart(), run.getPeriodEnd());

        Integer daysWorked = locked.get(employee.getId());
        if (daysWorked == null) {
            throw new InvalidOperationException(
                    "Days worked have not been locked for " + employee.getFullName() + " for "
                            + run.getPeriodStart() + " to " + run.getPeriodEnd());
        }

        return calculate(employee, run, daysWorked).payslip();
    }

    @Override
    public List<Payslip> calculateForEmployees(List<Employee> employees, PayrollRun run) {
        return employees.stream()
                .map(employee -> calculateForEmployee(employee, run))
                .toList();
    }

    // Overtime pay = hours worked under an OVERTIME-status attendance record, valued at
    // (daily rate / standard hours in a day) x the overtime multiplier from company settings.
    private BigDecimal calculateOvertimePay(Employee employee, PayrollRun run, CompanySettings settings) {
        List<Attendance> overtimeRecords = attendanceRepository
                .findByEmployeeIdAndAttendanceDateBetween(employee.getId(), run.getPeriodStart(), run.getPeriodEnd())
                .stream()
                .filter(a -> "OVERTIME".equals(a.getStatus()) && a.getClockIn() != null && a.getClockOut() != null)
                .toList();

        if (overtimeRecords.isEmpty()) {
            return BigDecimal.ZERO;
        }

        Double hoursPerDay = settings.getDefaultWorkingHoursPerDay();
        if (hoursPerDay == null || hoursPerDay <= 0) {
            return BigDecimal.ZERO;
        }

        double totalOvertimeHours = overtimeRecords.stream()
                .mapToDouble(a -> Duration.between(a.getClockIn(), a.getClockOut()).toMinutes() / 60.0)
                .sum();

        BigDecimal hourlyRate = settings.getDailyRate()
                .divide(BigDecimal.valueOf(hoursPerDay), 4, RoundingMode.HALF_UP);
        BigDecimal overtimeMultiplier = BigDecimal.valueOf(settings.getOvertimeMultiplier());

        return hourlyRate.multiply(BigDecimal.valueOf(totalOvertimeHours))
                .multiply(overtimeMultiplier)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private CompanySettings currentSettings() {
        return companySettingsRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Company settings have not been configured yet"));
    }
}