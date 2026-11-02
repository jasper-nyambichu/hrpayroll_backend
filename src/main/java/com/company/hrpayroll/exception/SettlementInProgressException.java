// exception/SettlementInProgressException.java
package com.company.hrpayroll.exception;

public class SettlementInProgressException extends RuntimeException {
    public SettlementInProgressException(String message) {
        super(message);
    }
}