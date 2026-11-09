package server;

import exception.IncompleteLineException;
import exception.MaxLengthExceededException;
import java.io.IOException;
import java.io.Reader;
import java.util.Objects;

/** Reads newline-delimited messages without taking ownership of the supplied reader. */
final class NewlineMessageReader {

  private final Reader reader;
  private final int maxMessageLength;

  NewlineMessageReader(Reader reader, int maxMessageLength) {
    this.reader = Objects.requireNonNull(reader);
    if (maxMessageLength <= 0) {
      throw new IllegalArgumentException("maxMessageLength must be positive");
    }
    this.maxMessageLength = maxMessageLength;
  }

  /** Returns a message without its delimiter, or null for EOF at a message boundary. */
  String readMessage() throws IOException {
    StringBuilder message = new StringBuilder();
    int character;
    while ((character = reader.read()) != -1) {
      if (character == '\n') {
        return message.toString();
      }
      // The character just read would exceed the limit; do not wait for a delimiter or EOF.
      if (message.length() == maxMessageLength) {
        throw new MaxLengthExceededException(
            "message exceeded max length of " + maxMessageLength + " characters");
      }
      message.append((char) character);
    }
    if (!message.isEmpty()) {
      throw new IncompleteLineException("Last message is incomplete: " + message);
    }
    return null;
  }
}
