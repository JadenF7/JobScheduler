package com.jadenf7.jobscheduler.domain.exception;

public class NoHandlerFoundException extends NonRetryableTaskException {

    public NoHandlerFoundException(String taskType) {
        super("No TaskHandler registered for task type: " + taskType);
    }
}
