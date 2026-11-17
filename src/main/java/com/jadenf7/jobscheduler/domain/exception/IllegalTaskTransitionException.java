package com.jadenf7.jobscheduler.domain.exception;

public class IllegalTaskTransitionException extends RuntimeException {
  public IllegalTaskTransitionException(String message) {
    super(message);
  }
}
