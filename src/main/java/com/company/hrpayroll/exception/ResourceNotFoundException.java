// exception/ResourceNotFoundException.java
package com.company.hrpayroll.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}