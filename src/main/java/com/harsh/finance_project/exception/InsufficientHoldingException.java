package com.harsh.finance_project.exception;

public class InsufficientHoldingException extends RuntimeException {
    public InsufficientHoldingException(String message) {
        super(message);
    }
}
