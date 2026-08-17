// exception/PayrollRunInProgressException.java
package com.company.hrpayroll.exception;

public class PayrollRunInProgressException extends RuntimeException {
    public PayrollRunInProgressException(String message) {
        super(message);
    }
}