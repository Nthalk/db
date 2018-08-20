package com.iodesystems.db.errors;

public class RollbackException extends Exception {

  public RollbackException(Throwable cause) {
    super(cause);
  }

  public RollbackException() {
  }

  public RollbackException(String message) {
    super(message);
  }

  public RollbackException(String message, Throwable cause) {
    super(message, cause);
  }
}
