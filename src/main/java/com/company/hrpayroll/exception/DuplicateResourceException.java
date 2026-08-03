// exception/DuplicateResourceException.java
package com.company.hrpayroll.exception;

public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}