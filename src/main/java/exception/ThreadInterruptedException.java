package exception;

public class ThreadInterruptedException extends RuntimeException {
  public ThreadInterruptedException(String message) {
    super(message);
  }
}
