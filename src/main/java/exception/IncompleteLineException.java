package exception;

public class IncompleteLineException extends RuntimeException {
  public IncompleteLineException(String message) {
    super(message);
  }
}
