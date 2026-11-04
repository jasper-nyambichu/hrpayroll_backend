// service/common/pdf/DateLabel.java
package com.company.hrpayroll.service.common.pdf;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

// Dates as people read them, in documents and messages: "04 Nov 2026". The API itself keeps yyyy-MM-dd.
public class DateLabel {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);

    private DateLabel() {}

    public static String format(LocalDate date) {
        return date == null ? "-" : FORMAT.format(date);
    }

    public static String range(LocalDate start, LocalDate end) {
        return format(start) + " to " + format(end);
    }
}
