package exception;

public class MaxLengthExceededException extends RuntimeException {
  public MaxLengthExceededException(String message) {
    super(message);
  }
}
