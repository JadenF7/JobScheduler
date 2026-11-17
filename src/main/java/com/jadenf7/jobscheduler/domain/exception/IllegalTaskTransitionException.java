package com.jadenf7.jobscheduler.domain.exception;

import com.jadenf7.jobscheduler.domain.model.TaskStatus;

public class IllegalTaskTransitionException extends RuntimeException {

    public IllegalTaskTransitionException(TaskStatus from, TaskStatus to) {
        super("Illegal transition from " + from + " to " + to);
    }
}
