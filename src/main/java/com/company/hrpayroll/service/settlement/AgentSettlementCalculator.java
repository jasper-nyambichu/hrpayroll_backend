// service/settlement/AgentSettlementCalculator.java
package com.company.hrpayroll.service.settlement;

import com.company.hrpayroll.entity.agent.Agent;
import com.company.hrpayroll.entity.agent.ChargeType;
import com.company.hrpayroll.entity.company.CompanySettings;
import com.company.hrpayroll.service.damage.DamageService;
import com.company.hrpayroll.service.loan.LoanService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The payment formula, and nothing else — no database and no state changes:
 *
 *   subtotal = (days worked x daily rate) + PAYE + NSSF + NITA + agent fee
 *   VAT      = subtotal x VAT rate
 *   payable  = subtotal + VAT - loans - damages - advances
 *
 * VAT is charged before loans, damages and advances come off. An advance can never take the
 * payable amount below zero; whatever does not fit stays outstanding for the next period.
 */
@Component
@RequiredArgsConstructor
public class AgentSettlementCalculator {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final StatutoryDeductionCalculator statutoryDeductionCalculator;

    /** One employee's share of the settlement, with the loans and damages it actually took. */
    public record EmployeeLine(
            int daysWorked,
            BigDecimal pay,
            BigDecimal paye,
            BigDecimal nssf,
            BigDecimal nita,
            List<LoanService.LoanDeductionResult> loans,
            List<DamageService.DamageDeductionResult> damages
    ) {
        public BigDecimal loanTotal() {
            return loans.stream().map(LoanService.LoanDeductionResult::installmentAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        public BigDecimal damageTotal() {
            return damages.stream().map(DamageService.DamageDeductionResult::amount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        // What the employee is paid. The agent's amount is the sum of these + agent fee + VAT - advances.
        public BigDecimal amountDue() {
            return pay.add(paye).add(nssf).add(nita).subtract(loanTotal()).subtract(damageTotal()).max(BigDecimal.ZERO);
        }
    }

    public record Totals(
            int daysWorked,
            BigDecimal daysPay,
            BigDecimal paye,
            BigDecimal nssf,
            BigDecimal nita,
            BigDecimal agentFee,
            BigDecimal vatRate,
            BigDecimal vat,
            BigDecimal loans,
            BigDecimal damages,
            BigDecimal advance,
            BigDecimal payable
    ) {}

    // Loans and damages share the deduction ceiling, loans first. A damage is taken whole or not at
    // all: one that does not fit waits for the next period.
    public EmployeeLine calculateEmployee(
            CompanySettings settings,
            int daysWorked,
            List<LoanService.LoanDeductionResult> dueLoans,
            List<DamageService.DamageDeductionResult> approvedDamages
    ) {
        BigDecimal pay = settings.getDailyRate().multiply(BigDecimal.valueOf(daysWorked)).setScale(2, RoundingMode.HALF_UP);

        BigDecimal nssf = statutoryDeductionCalculator.calculateConfigured(pay, settings.getNssfMode(), settings.getNssfValue());
        BigDecimal paye = statutoryDeductionCalculator.calculatePaye(pay, nssf, BigDecimal.ZERO, BigDecimal.ZERO);
        BigDecimal nita = daysWorked > 0 ? statutoryDeductionCalculator.calculateNita() : BigDecimal.ZERO;

        BigDecimal remaining = pay.multiply(settings.getDeductionCeilingPercent()).divide(HUNDRED, 2, RoundingMode.HALF_UP);

        List<LoanService.LoanDeductionResult> loans = new ArrayList<>();
        for (LoanService.LoanDeductionResult due : dueLoans) {
            BigDecimal amount = due.installmentAmount().min(remaining);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            loans.add(new LoanService.LoanDeductionResult(due.loanId(), due.employeeId(), amount));
            remaining = remaining.subtract(amount);
        }

        List<DamageService.DamageDeductionResult> damages = new ArrayList<>();
        List<DamageService.DamageDeductionResult> oldestFirst = approvedDamages.stream()
                .sorted(Comparator.comparing(DamageService.DamageDeductionResult::damageId))
                .toList();
        for (DamageService.DamageDeductionResult damage : oldestFirst) {
            if (damage.amount().compareTo(remaining) > 0) {
                continue;
            }
            damages.add(damage);
            remaining = remaining.subtract(damage.amount());
        }

        return new EmployeeLine(daysWorked, pay, paye, nssf, nita, loans, damages);
    }

    public Totals calculateTotals(Agent agent, List<EmployeeLine> lines, BigDecimal vatRate, BigDecimal advanceBalance) {
        int days = lines.stream().mapToInt(EmployeeLine::daysWorked).sum();
        BigDecimal daysPay = sum(lines, EmployeeLine::pay);
        BigDecimal paye = sum(lines, EmployeeLine::paye);
        BigDecimal nssf = sum(lines, EmployeeLine::nssf);
        BigDecimal nita = sum(lines, EmployeeLine::nita);
        BigDecimal loans = sum(lines, EmployeeLine::loanTotal);
        BigDecimal damages = sum(lines, EmployeeLine::damageTotal);

        BigDecimal agentFee = calculateAgentFee(agent, lines.size(), daysPay);

        BigDecimal subtotal = daysPay.add(paye).add(nssf).add(nita).add(agentFee);
        BigDecimal vat = subtotal.multiply(vatRate).divide(HUNDRED, 2, RoundingMode.HALF_UP);

        BigDecimal beforeAdvance = subtotal.add(vat).subtract(loans).subtract(damages).max(BigDecimal.ZERO);
        BigDecimal advance = advanceBalance.min(beforeAdvance);
        BigDecimal payable = beforeAdvance.subtract(advance).setScale(2, RoundingMode.HALF_UP);

        return new Totals(days, daysPay, paye, nssf, nita, agentFee, vatRate, vat, loans, damages, advance, payable);
    }

    // Charge amount / percentage are nullable columns (only one of them is used per charge type), so
    // a missing value is treated as zero.
    private BigDecimal calculateAgentFee(Agent agent, int employeeCount, BigDecimal daysPay) {
        ChargeType type = agent.getChargeType();
        BigDecimal chargeAmount = agent.getChargeAmount() != null ? agent.getChargeAmount() : BigDecimal.ZERO;
        BigDecimal chargePercentage = agent.getChargePercentage() != null ? agent.getChargePercentage() : BigDecimal.ZERO;

        return switch (type) {
            case PER_EMPLOYEE -> chargeAmount
                    .multiply(BigDecimal.valueOf(employeeCount))
                    .setScale(2, RoundingMode.HALF_UP);

            case PERCENTAGE_OF_GROSS -> daysPay
                    .multiply(chargePercentage)
                    .divide(HUNDRED, 2, RoundingMode.HALF_UP);

            case FIXED_MONTHLY, CUSTOM -> chargeAmount.setScale(2, RoundingMode.HALF_UP);
        };
    }

    private BigDecimal sum(List<EmployeeLine> lines, java.util.function.Function<EmployeeLine, BigDecimal> extractor) {
        return lines.stream().map(extractor).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
