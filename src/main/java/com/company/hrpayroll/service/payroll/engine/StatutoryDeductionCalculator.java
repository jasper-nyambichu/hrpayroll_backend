// service/payroll/engine/StatutoryDeductionCalculator.java
package com.company.hrpayroll.service.payroll.engine;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Kenyan statutory deduction calculations.
 *
 * IMPORTANT: the rates, caps, and PAYE bands below are illustrative
 * placeholders reflecting known structures at the time this was written.
 * These figures change periodically by government notice — confirm current
 * SHIF/NSSF/Housing Levy rates and PAYE bands/personal relief before this
 * is used on a real payroll run. This class is deliberately the ONLY place
 * these numbers appear, so an update later is a one-file change.
 */
@Component
public class StatutoryDeductionCalculator {

    private static final BigDecimal SHIF_RATE = new BigDecimal("0.0275");
    private static final BigDecimal SHIF_MINIMUM = new BigDecimal("300");

    private static final BigDecimal NSSF_RATE = new BigDecimal("0.06");
    private static final BigDecimal NSSF_PENSIONABLE_CAP = new BigDecimal("8000");

    private static final BigDecimal HOUSING_LEVY_RATE = new BigDecimal("0.015");

    private static final BigDecimal NITA_FLAT_AMOUNT = new BigDecimal("50");

    private static final BigDecimal PERSONAL_RELIEF = new BigDecimal("2400");

    public BigDecimal calculateShif(BigDecimal grossPay) {
        BigDecimal computed = grossPay.multiply(SHIF_RATE).setScale(2, RoundingMode.HALF_UP);
        return computed.max(SHIF_MINIMUM);
    }

    public BigDecimal calculateNssf(BigDecimal grossPay) {
        BigDecimal pensionablePay = grossPay.min(NSSF_PENSIONABLE_CAP);
        return pensionablePay.multiply(NSSF_RATE).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateHousingLevy(BigDecimal grossPay) {
        return grossPay.multiply(HOUSING_LEVY_RATE).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateNita() {
        return NITA_FLAT_AMOUNT;
    }

    // Taxable pay = gross - NSSF (NSSF is an allowable pre-tax deduction).
    // Applied as a progressive band system: each band taxes only the slice
    // of income that falls within it, not the whole taxable pay at that rate.
    public BigDecimal calculatePaye(BigDecimal grossPay, BigDecimal nssfDeduction) {
        BigDecimal remaining = grossPay.subtract(nssfDeduction).max(BigDecimal.ZERO);
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