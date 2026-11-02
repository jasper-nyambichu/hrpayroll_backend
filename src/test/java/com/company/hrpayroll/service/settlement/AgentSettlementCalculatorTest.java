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

    // Default settings: 1,000 a day, NSSF 6% of pay, 66.67% deduction ceiling.
    private static CompanySettings settings() {
        return CompanySettings.builder().build();
    }

    @Test
    void oneEmployeeForThirtyDaysFollowsTheFormula() {
        AgentSettlementCalculator.EmployeeLine line = calculator.calculateEmployee(settings(), 30, List.of(), List.of());

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
    void vatIsChargedBeforeLoansDamagesAndAdvancesComeOff() {
        AgentSettlementCalculator.EmployeeLine line = calculator.calculateEmployee(
                settings(),
                30,
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
        AgentSettlementCalculator.EmployeeLine line = calculator.calculateEmployee(settings(), 30, List.of(), List.of());

        AgentSettlementCalculator.Totals totals =
                calculator.calculateTotals(agentCharging("500"), List.of(line), kes("16"), kes("50000"));

        assertKes("38744.00", totals.advance());
        assertKes("0.00", totals.payable());
    }

    @Test
    void thePayslipIsPayPlusStatutoryLessLoansAndDamages() {
        AgentSettlementCalculator.EmployeeLine line = calculator.calculateEmployee(
                settings(),
                30,
                List.of(new LoanService.LoanDeductionResult(1L, 1L, kes("2000"))),
                List.of());

        // 30,000 + 1,050 + 1,800 + 50 - 2,000
        assertKes("30900.00", line.amountDue());
    }

    @Test
    void theAgentsAmountIsThePayslipsPlusFeeAndVatLessAdvances() {
        List<AgentSettlementCalculator.EmployeeLine> lines = List.of(
                calculator.calculateEmployee(settings(), 30, List.of(new LoanService.LoanDeductionResult(1L, 1L, kes("2000"))), List.of()),
                calculator.calculateEmployee(settings(), 20, List.of(), List.of(new DamageService.DamageDeductionResult(1L, 2L, kes("500")))));

        AgentSettlementCalculator.Totals totals =
                calculator.calculateTotals(agentCharging("500"), lines, kes("16"), kes("1000"));

        BigDecimal payslips = lines.stream()
                .map(AgentSettlementCalculator.EmployeeLine::amountDue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertEquals(0, payslips.add(totals.agentFee()).add(totals.vat()).subtract(totals.advance()).compareTo(totals.payable()));
    }

    @Test
    void noDaysWorkedChargesNothingForThatEmployee() {
        AgentSettlementCalculator.EmployeeLine line = calculator.calculateEmployee(settings(), 0, List.of(), List.of());

        assertKes("0", line.pay());
        assertKes("0", line.paye());
        assertKes("0", line.nssf());
        assertKes("0", line.nita());
    }

    @Test
    void loansAreCappedAndADamageThatDoesNotFitWaits() {
        AgentSettlementCalculator.EmployeeLine line = calculator.calculateEmployee(
                settings(),
                10,
                List.of(new LoanService.LoanDeductionResult(1L, 1L, kes("9000"))),
                List.of(new DamageService.DamageDeductionResult(1L, 1L, kes("500"))));

        // Ceiling is 66.67% of 10,000 = 6,667: the loan takes it all, so the damage waits.
        assertKes("6667.00", line.loanTotal());
        assertTrue(line.damages().isEmpty());
    }
}
