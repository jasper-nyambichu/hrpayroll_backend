// service/employee/EmployeeNames.java
package com.company.hrpayroll.service.employee;

import com.company.hrpayroll.exception.InvalidOperationException;

import java.util.regex.Pattern;

/**
 * A new employee is recorded with a first name and a last name only: two names, letters with an optional hyphen or
 * apostrophe inside (Mary-Jane, O'Neil). Checked when an employee is added or proposed; employees already on record
 * (and requests already waiting) keep the name they have.
 */
public final class EmployeeNames {

    private static final Pattern ONE_NAME = Pattern.compile("\\p{L}+(?:['\u2019-]\\p{L}+)*");

    private EmployeeNames() {}

    public static void requireFirstAndLast(String fullName) {
        String[] parts = fullName == null ? new String[0] : fullName.trim().split("\\s+");
        if (parts.length != 2 || parts[0].isEmpty()
                || !ONE_NAME.matcher(parts[0]).matches() || !ONE_NAME.matcher(parts[1]).matches()
                || parts[0].length() > 40 || parts[1].length() > 40) {
            throw new InvalidOperationException("Enter the employee's first name and last name only (one name each)");
        }
    }
}
