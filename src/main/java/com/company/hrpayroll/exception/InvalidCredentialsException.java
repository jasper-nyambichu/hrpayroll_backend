// exception/InvalidCredentialsException.java
package com.company.hrpayroll.exception;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String message) {
        super(message);
    }
}