// service/payroll/agentformula/impl/AgentPayrollCalculationServiceImpl.java
package com.company.hrpayroll.service.payroll.agentformula.impl;

import com.company.hrpayroll.entity.agent.Agent;
import com.company.hrpayroll.entity.agent.ChargeType;
import com.company.hrpayroll.entity.employee.Employee;
import com.company.hrpayroll.entity.payroll.AgentPayrollSummary;
import com.company.hrpayroll.entity.payroll.PayrollRun;
import com.company.hrpayroll.entity.payroll.Payslip;
import com.company.hrpayroll.service.company.CompanySettingsService;
import com.company.hrpayroll.service.payroll.agentformula.AgentPayrollCalculationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AgentPayrollCalculationServiceImpl implements AgentPayrollCalculationService {

    private final CompanySettingsService companySettingsService;

    @Override
    public AgentPayrollSummary calculateForAgent(
            Agent agent,
            List<Employee> agentEmployees,
            List<Payslip> agentPayslips,
            BigDecimal recoverableAdvanceBalance,
            PayrollRun run
    ) {
        int employeeCount = agentEmployees.size();

        BigDecimal grossSalaries = sumField(agentPayslips, Payslip::getGrossPay);
        BigDecimal shifTotal = sumField(agentPayslips, Payslip::getShifDeduction);
        BigDecimal nssfTotal = sumField(agentPayslips, Payslip::getNssfDeduction);
        BigDecimal housingLevyTotal = sumField(agentPayslips, Payslip::getHousingLevyDeduction);
        BigDecimal payeTotal = sumField(agentPayslips, Payslip::getPayeDeduction);
        BigDecimal loanDeductionsTotal = sumField(agentPayslips, Payslip::getLoanDeduction);

        BigDecimal serviceCharge = calculateServiceCharge(agent, employeeCount, grossSalaries);

        // Recovery is capped at what's actually payable — an agent's
        // advance balance can never push the amount owed below what they
        // are entitled to for this run.
        BigDecimal grossBeforeAdvanceCap = grossSalaries.add(serviceCharge).add(shifTotal)
                .add(nssfTotal).add(housingLevyTotal).add(payeTotal).add(loanDeductionsTotal);
        BigDecimal advanceRecovery = recoverableAdvanceBalance.min(grossBeforeAdvanceCap);

        // Exactly the formula from proposal §6:
        // Gross Salaries + Service Charge + SHIF + NSSF + Housing Levy + PAYE
        // + Employee Loan Deductions − Agent Advance Recovery = Subtotal Before VAT
        BigDecimal subtotalBeforeVat = grossSalaries
                .add(serviceCharge)
                .add(shifTotal)
                .add(nssfTotal)
                .add(housingLevyTotal)
                .add(payeTotal)
                .add(loanDeductionsTotal)
                .subtract(advanceRecovery)
                .setScale(2, RoundingMode.HALF_UP);

        boolean vatApplied = agent.isVatRegistered();
        BigDecimal vatRateUsed = vatApplied ? companySettingsService.getCurrentVatRate() : BigDecimal.ZERO;
        BigDecimal vatAmount = vatApplied
                ? subtotalBeforeVat.multiply(vatRateUsed)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal finalAmountPayable = subtotalBeforeVat.add(vatAmount).setScale(2, RoundingMode.HALF_UP);

        return AgentPayrollSummary.builder()
                .payrollRunId(run.getId())
                .agentId(agent.getId())
                .employeeCount(employeeCount)
                .grossSalaries(grossSalaries)
                .serviceCharge(serviceCharge)
                .shifTotal(shifTotal)
                .nssfTotal(nssfTotal)
                .housingLevyTotal(housingLevyTotal)
                .payeTotal(payeTotal)
                .employeeLoanDeductionsTotal(loanDeductionsTotal)
                .advanceRecoveryTotal(advanceRecovery)
                .subtotalBeforeVat(subtotalBeforeVat)
                .vatApplied(vatApplied)
                .vatRateUsed(vatRateUsed)
                .vatAmount(vatAmount)
                .finalAmountPayable(finalAmountPayable)
                .build();
    }

    private BigDecimal calculateServiceCharge(Agent agent, int employeeCount, BigDecimal grossSalaries) {
        ChargeType type = agent.getChargeType();

        return switch (type) {
            case PER_EMPLOYEE -> agent.getChargeAmount()
                    .multiply(BigDecimal.valueOf(employeeCount))
                    .setScale(2, RoundingMode.HALF_UP);

            case PERCENTAGE_OF_GROSS -> grossSalaries
                    .multiply(agent.getChargePercentage())
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

            case FIXED_MONTHLY, CUSTOM -> agent.getChargeAmount().setScale(2, RoundingMode.HALF_UP);
        };
    }

    private BigDecimal sumField(List<Payslip> payslips, java.util.function.Function<Payslip, BigDecimal> extractor) {
        return payslips.stream()
                .map(extractor)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}