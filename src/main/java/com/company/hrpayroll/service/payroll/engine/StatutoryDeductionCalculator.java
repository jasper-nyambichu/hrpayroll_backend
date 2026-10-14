// service/payroll/engine/StatutoryDeductionCalculator.java
package com.company.hrpayroll.service.payroll.engine;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Kenyan statutory deduction calculations.
 *
 * Rates and limits below reflect the position in October 2026:
 *  - SHIF 2.75% of gross, minimum KES 300, no upper cap.
 *  - NSSF Year 4 (effective 1 February 2026): 6% of pensionable pay up to the
 *    Upper Earnings Limit of KES 108,000 (maximum KES 6,480). The limits step up
 *    every February, so UPDATE NSSF_UPPER_EARNINGS_LIMIT each year.
 *  - Affordable Housing Levy 1.5% of gross, no cap.
 *  - NITA KES 50 per employee per month. This is an EMPLOYER levy, not an
 *    employee deduction (the Industrial Training Act forbids deducting it).
 *  - PAYE: taxable pay = gross - NSSF - SHIF - Housing Levy (all allowable since
 *    the Tax Laws (Amendment) Act 2024), progressive bands, less KES 2,400 relief.
 *
 * This class is deliberately the ONLY place these numbers appear, so an update
 * later is a one-file change.
 */
@Component
public class StatutoryDeductionCalculator {

    private static final BigDecimal SHIF_RATE = new BigDecimal("0.0275");
    private static final BigDecimal SHIF_MINIMUM = new BigDecimal("300");

    private static final BigDecimal NSSF_RATE = new BigDecimal("0.06");
    private static final BigDecimal NSSF_UPPER_EARNINGS_LIMIT = new BigDecimal("108000");

    private static final BigDecimal HOUSING_LEVY_RATE = new BigDecimal("0.015");

    private static final BigDecimal NITA_FLAT_AMOUNT = new BigDecimal("50");

    private static final BigDecimal PERSONAL_RELIEF = new BigDecimal("2400");

    public BigDecimal calculateShif(BigDecimal grossPay) {
        BigDecimal computed = grossPay.multiply(SHIF_RATE).setScale(2, RoundingMode.HALF_UP);
        return computed.max(SHIF_MINIMUM);
    }

    // Tier I + Tier II together are simply 6% of pay up to the Upper Earnings Limit.
    public BigDecimal calculateNssf(BigDecimal grossPay) {
        BigDecimal pensionablePay = grossPay.min(NSSF_UPPER_EARNINGS_LIMIT);
        return pensionablePay.multiply(NSSF_RATE).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateHousingLevy(BigDecimal grossPay) {
        return grossPay.multiply(HOUSING_LEVY_RATE).setScale(2, RoundingMode.HALF_UP);
    }

    // Employer levy: calculated and recorded on the payslip, never deducted from net pay.
    public BigDecimal calculateNita() {
        return NITA_FLAT_AMOUNT;
    }

    // Taxable pay = gross - NSSF - SHIF - Housing Levy (all allowable pre-tax deductions).
    // Applied as a progressive band system: each band taxes only the slice
    // of income that falls within it, not the whole taxable pay at that rate.
    public BigDecimal calculatePaye(
            BigDecimal grossPay,
            BigDecimal nssfDeduction,
            BigDecimal shifDeduction,
            BigDecimal housingLevyDeduction
    ) {
        BigDecimal remaining = grossPay
                .subtract(nssfDeduction)
                .subtract(shifDeduction)
                .subtract(housingLevyDeduction)
                .max(BigDecimal.ZERO);
        BigDecimal tax = BigDecimal.ZERO;

        tax = tax.add(taxBand(remaining, new BigDecimal("24000"), new BigDecimal("0.10")));
        remaining = remaining.subtract(new BigDecimal("24000")).max(BigDecimal.ZERO);

        tax = tax.add(taxBand(remaining, new BigDecimal("8333"), new BigDecimal("0.25")));
        remaining = remaining.subtract(new BigDecimal("8333")).max(BigDecimal.ZERO);

        tax = tax.add(taxBand(remaining, new BigDecimal("467667"), new BigDecimal("0.30")));
        remaining = remaining.subtract(new BigDecimal("467667")).max(BigDecimal.ZERO);

        tax = tax.add(taxBand(remaining, new BigDecimal("300000"), new BigDecimal("0.325")));
        remaining = remaining.subtract(new BigDecimal("300000")).max(BigDecimal.ZERO);

        tax = tax.add(remaining.multiply(new BigDecimal("0.35"))); // top band, uncapped

        BigDecimal payeAfterRelief = tax.subtract(PERSONAL_RELIEF).max(BigDecimal.ZERO);
        return payeAfterRelief.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal taxBand(BigDecimal remainingTaxablePay, BigDecimal bandWidth, BigDecimal rate) {
        BigDecimal taxableInThisBand = remainingTaxablePay.min(bandWidth);
        if (taxableInThisBand.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return taxableInThisBand.multiply(rate);
    }
}