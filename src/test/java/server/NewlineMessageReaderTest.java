package server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static util.Constants.MAX_MESSAGE_LENGTH;

import exception.IncompleteLineException;
import exception.MaxLengthExceededException;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@Timeout(5)
class NewlineMessageReaderTest {

  @Test
  void returnsMessageWithoutDelimiter() throws IOException {
    NewlineMessageReader reader = readerFor("device_01,0,23.5\n");

    assertEquals("device_01,0,23.5", reader.readMessage());
    assertNull(reader.readMessage());
  }

  @Test
  void preservesSuccessiveMessagesIncludingAnEmptyMessage() throws IOException {
    NewlineMessageReader reader = readerFor("first\n\nthird\n");

    assertEquals("first", reader.readMessage());
    assertEquals("", reader.readMessage());
    assertEquals("third", reader.readMessage());
    assertNull(reader.readMessage());
  }

  @Test
  void acceptsEofWithoutAnyData() throws IOException {
    assertNull(readerFor("").readMessage());
  }

  @ParameterizedTest
  @ValueSource(ints = {255, 256})
  void acceptsMessagesUpToTheLimitExcludingDelimiter(int length) throws IOException {
    String message = "x".repeat(length);
    NewlineMessageReader reader = readerFor(message + "\n");

    assertEquals(message, reader.readMessage());
    assertNull(reader.readMessage());
  }

  @Test
  void rejectsThe257thCharacterWithoutReadingDelimiterOrEof() {
    Reader input =
        new StringReader("x".repeat(MAX_MESSAGE_LENGTH + 1)) {
          @Override
          public int read() throws IOException {
            int character = super.read();
            if (character == -1) {
              fail("Oversized messages must be rejected before waiting for EOF");
            }
            return character;
          }
        };

    assertThrows(
        MaxLengthExceededException.class,
        () -> new NewlineMessageReader(input, MAX_MESSAGE_LENGTH).readMessage());
  }

  @Test
  void appliesTheLimitIndependentlyToEachMessage() throws IOException {
    String message = "x".repeat(MAX_MESSAGE_LENGTH);
    NewlineMessageReader reader = readerFor(message + "\n" + message + "\n");

    assertEquals(message, reader.readMessage());
    assertEquals(message, reader.readMessage());
    assertNull(reader.readMessage());
  }

  @Test
  void rejectsAnIncompleteMessageAtEof() {
    assertThrows(IncompleteLineException.class, () -> readerFor("partial").readMessage());
  }

  @Test
  void rejectsAnIncompleteMessageEvenWhenItHasExactly256Characters() {
    NewlineMessageReader reader = readerFor("x".repeat(MAX_MESSAGE_LENGTH));

    assertThrows(IncompleteLineException.class, reader::readMessage);
  }

  @Test
  void deliversCompletedMessageBeforeRejectingTrailingPartialMessage() throws IOException {
    NewlineMessageReader reader = readerFor("complete\npartial");

    assertEquals("complete", reader.readMessage());
    assertThrows(IncompleteLineException.class, reader::readMessage);
  }

  @Test
  void countsJavaCharsRatherThanUnicodeCodePoints() throws IOException {
    String message = "\uD83D\uDE80".repeat(MAX_MESSAGE_LENGTH / 2);

    assertEquals(message, readerFor(message + "\n").readMessage());
    assertThrows(
        MaxLengthExceededException.class, () -> readerFor(message + "x\n").readMessage());
  }

  @Test
  void propagatesReadFailuresRatherThanTreatingThemAsEof() {
    IOException failure = new IOException("simulated read failure");
    Reader input =
        new Reader() {
          @Override
          public int read(char[] buffer, int offset, int length) throws IOException {
            throw failure;
          }

          @Override
          public void close() {}
        };

    IOException actual =
        assertThrows(
            IOException.class,
            () -> new NewlineMessageReader(input, MAX_MESSAGE_LENGTH).readMessage());
    assertSame(failure, actual);
  }

  private static NewlineMessageReader readerFor(String input) {
    return new NewlineMessageReader(new StringReader(input), MAX_MESSAGE_LENGTH);
  }
}
