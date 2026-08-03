// exception/InvalidOperationException.java
package com.company.hrpayroll.exception;

// For business-rule violations that aren't "not found" or "duplicate" —
// e.g. a charge type missing its required amount, or over-recovering an advance.
public class InvalidOperationException extends RuntimeException {
    public InvalidOperationException(String message) {
        super(message);
    }
}