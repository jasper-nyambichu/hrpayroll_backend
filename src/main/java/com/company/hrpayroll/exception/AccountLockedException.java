// exception/AccountLockedException.java
package com.company.hrpayroll.exception;

public class AccountLockedException extends RuntimeException {
    public AccountLockedException(String message) {
        super(message);
    }
}