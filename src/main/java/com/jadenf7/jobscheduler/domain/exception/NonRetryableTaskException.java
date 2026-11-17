package com.jadenf7.jobscheduler.domain.exception;

public class NonRetryableTaskException extends RuntimeException {
    public NonRetryableTaskException(String message) {
        super(message);
    }
}
