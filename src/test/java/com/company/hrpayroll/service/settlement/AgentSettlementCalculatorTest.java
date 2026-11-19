package com.company.hrpayroll.service.settlement;

import com.company.hrpayroll.entity.agent.Agent;
import com.company.hrpayroll.entity.agent.ChargeType;
import com.company.hrpayroll.entity.company.CompanySettings;
import com.company.hrpayroll.service.damage.DamageService;
import com.company.hrpayroll.service.loan.LoanService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentSettlementCalculatorTest {

    private final AgentSettlementCalculator calculator = new AgentSettlementCalculator(new StatutoryDeductionCalculator());

    private static BigDecimal kes(String amount) {
        return new BigDecimal(amount);
    }

    private static void assertKes(String expected, BigDecimal actual) {
        assertEquals(0, kes(expected).compareTo(actual), () -> "expected " + expected + " but was " + actual);
    }

    private static Agent agentCharging(String perEmployee) {
        return Agent.builder().name("Agent").chargeType(ChargeType.PER_EMPLOYEE).chargeAmount(kes(perEmployee)).build();
    }

    // Default settings: NSSF 6% of pay, 66.67% deduction ceiling.
    private static CompanySettings settings() {
        return CompanySettings.builder().build();
    }

    // An employee on a monthly salary, in a 30-day month paid as one period.
    private AgentSettlementCalculator.EmployeeLine line(String salary, int worked, int off,
                                                        List<LoanService.LoanDeductionResult> loans,
                                                        List<DamageService.DamageDeductionResult> damages) {
        return calculator.calculateEmployee(settings(), salary == null ? null : kes(salary), worked, off, 30, 30, loans, damages);
    }

    @Test
    void aFullMonthPaysTheWholeSalaryAndFollowsTheFormula() {
        AgentSettlementCalculator.EmployeeLine line = line("30000", 30, 0, List.of(), List.of());

        assertKes("30000.00", line.pay());
        assertKes("1800.00", line.nssf());
        assertKes("50", line.nita());
        // Taxable 28,200: 2,400 + (4,200 x 25%) = 3,450, less 2,400 relief.
        assertKes("1050.00", line.paye());

        AgentSettlementCalculator.Totals totals =
                calculator.calculateTotals(agentCharging("500"), List.of(line), kes("16"), BigDecimal.ZERO);

        // 30,000 + 1,050 + 1,800 + 50 + 500 = 33,400; VAT 16% = 5,344.
        assertKes("5344.00", totals.vat());
        assertKes("38744.00", totals.payable());
    }

    @Test
    void eachEmployeesOwnSalaryIsProratedByTheDaysPaidOffDaysIncluded() {
        // 45,000 a month; worked 25 days and had 2 off days in a 30-day month: 27 days -> 40,500.
        AgentSettlementCalculator.EmployeeLine line = line("45000", 25, 2, List.of(), List.of());
        assertKes("40500.00", line.pay());
        assertKes("1500.00", line.dailyRate());
        assertEquals(27, line.daysPaid());

        // Someone else on 18,000 for the same days is paid from their own salary.
        assertKes("16200.00", line("18000", 25, 2, List.of(), List.of()).pay());
    }

    @Test
    void aWholeLongMonthStillPaysExactlyTheSalary() {
        AgentSettlementCalculator.EmployeeLine line = calculator.calculateEmployee(settings(), kes("31000"), 31, 0, 31, 31, List.of(), List.of());
        assertKes("31000.00", line.pay());
    }

    @Test
    void anEmployeeWithoutASalaryIsPaidNothing() {
        AgentSettlementCalculator.EmployeeLine line = line(null, 30, 0, List.of(), List.of());
        assertKes("0", line.pay());
        assertKes("0", line.nita());
    }

    @Test
    void vatIsChargedBeforeLoansDamagesAndAdvancesComeOff() {
        AgentSettlementCalculator.EmployeeLine line = line("30000", 30, 0,
                List.of(new LoanService.LoanDeductionResult(1L, 1L, kes("2000"))),
                List.of(new DamageService.DamageDeductionResult(1L, 1L, kes("1000"))));

        AgentSettlementCalculator.Totals totals =
                calculator.calculateTotals(agentCharging("500"), List.of(line), kes("16"), kes("10000"));

        assertKes("5344.00", totals.vat());
        assertKes("2000", totals.loans());
        assertKes("1000", totals.damages());
        assertKes("10000", totals.advance());
        // 33,400 + 5,344 - 2,000 - 1,000 - 10,000
        assertKes("25744.00", totals.payable());
    }

    @Test
    void anAdvanceLargerThanThePaymentStopsAtZeroAndTheRestCarriesOver() {
        AgentSettlementCalculator.Totals totals =
                calculator.calculateTotals(agentCharging("500"), List.of(line("30000", 30, 0, List.of(), List.of())), kes("16"), kes("50000"));

        assertKes("38744.00", totals.advance());
        assertKes("0.00", totals.payable());
    }

    @Test
    void theNetPayIsTheSalaryLessLoansAndDamagesOnly() {
        AgentSettlementCalculator.EmployeeLine line = line("30000", 30, 0,
                List.of(new LoanService.LoanDeductionResult(1L, 1L, kes("2000"))),
                List.of(new DamageService.DamageDeductionResult(1L, 1L, kes("500"))));

        // 30,000 - 2,000 - 500: PAYE, NSSF and NITA are on the agent's invoice, not the employee's net pay.
        assertKes("27500.00", line.amountDue());
    }

    @Test
    void theAgentsAmountIsNetPaysPlusStatutoryFeeAndVatLessAdvances() {
        List<AgentSettlementCalculator.EmployeeLine> lines = List.of(
                line("30000", 30, 0, List.of(new LoanService.LoanDeductionResult(1L, 1L, kes("2000"))), List.of()),
                line("24000", 20, 0, List.of(), List.of(new DamageService.DamageDeductionResult(1L, 2L, kes("500")))));

        AgentSettlementCalculator.Totals totals =
                calculator.calculateTotals(agentCharging("500"), lines, kes("16"), kes("1000"));

        BigDecimal netPays = lines.stream().map(AgentSettlementCalculator.EmployeeLine::amountDue).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal expected = netPays.add(totals.paye()).add(totals.nssf()).add(totals.nita())
                .add(totals.agentFee()).add(totals.vat()).subtract(totals.advance());
        assertEquals(0, expected.compareTo(totals.payable()));
    }

    @Test
    void loansAreCappedAndADamageThatDoesNotFitWaits() {
        AgentSettlementCalculator.EmployeeLine line = line("30000", 10, 0,
                List.of(new LoanService.LoanDeductionResult(1L, 1L, kes("9000"))),
                List.of(new DamageService.DamageDeductionResult(1L, 1L, kes("500"))));

        // Pay 10,000; the ceiling is 66.67% of it = 6,667: the loan takes it all, so the damage waits.
        assertKes("6667.00", line.loanTotal());
        assertTrue(line.damages().isEmpty());
    }
}
