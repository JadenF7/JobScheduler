package com.jadenf7.jobscheduler.domain.exception;

public class NoHandlerFoundException extends RuntimeException {
  public NoHandlerFoundException(String message) {
    super(message);
  }
}
