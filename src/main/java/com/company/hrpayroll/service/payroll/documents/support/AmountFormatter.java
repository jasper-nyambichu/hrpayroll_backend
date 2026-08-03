// service/payroll/documents/support/AmountFormatter.java
package com.company.hrpayroll.service.payroll.documents.support;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public class AmountFormatter {

    private static final NumberFormat FORMAT = NumberFormat.getNumberInstance(Locale.US);
    static {
        FORMAT.setMinimumFractionDigits(2);
        FORMAT.setMaximumFractionDigits(2);
    }

    private AmountFormatter() {}

    public static String format(BigDecimal amount) {
        return amount == null ? "0.00" : FORMAT.format(amount);
    }
}