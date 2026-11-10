package client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import exception.ConnectionFailedException;
import exception.ThreadInterruptedException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

@Timeout(10)
class DeviceSimulatorTest {

  // These unit tests override Socket.connect; this address never receives network traffic.
  private static final InetSocketAddress ADDRESS = new InetSocketAddress("127.0.0.1", 19000);

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 3})
  void stopsAtTheFirstSuccessfulAttemptAndTransfersAnOpenSocket(int successAttempt)
      throws IOException {
    List<RecordingSocket> sockets = new ArrayList<>();
    for (int attempt = 1; attempt < successAttempt; attempt++) {
      sockets.add(new RecordingSocket(new ConnectException("refused")));
    }
    RecordingSocket success = new RecordingSocket(null);
    sockets.add(success);
    AtomicInteger created = new AtomicInteger();
    AtomicInteger pauses = new AtomicInteger();

    try (Socket actual =
        DeviceSimulator.tryToConnect(
            ADDRESS,
            factory(sockets, created),
            () -> {
              assertTrue(sockets.get(created.get() - 1).isClosed(), "Close before pausing");
              pauses.incrementAndGet();
            })) {
      assertSame(success, actual);
      assertFalse(actual.isClosed(), "The caller must receive an open socket");
      assertEquals(successAttempt, created.get());
      assertEquals(successAttempt - 1, pauses.get());
      for (RecordingSocket socket : sockets) {
        assertEquals(ADDRESS, socket.address);
        assertEquals(1000, socket.connectTimeoutMillis);
      }
    }
    assertTrue(success.isClosed());
  }

  @Test
  void retriesBothConnectTimeoutAndRefusalBeforeSucceeding() throws IOException {
    RecordingSocket timeout = new RecordingSocket(new SocketTimeoutException("timed out"));
    RecordingSocket refused = new RecordingSocket(new ConnectException("refused"));
    RecordingSocket success = new RecordingSocket(null);
    AtomicInteger created = new AtomicInteger();
    AtomicInteger pauses = new AtomicInteger();

    try (Socket actual =
        DeviceSimulator.tryToConnect(
            ADDRESS,
            factory(List.of(timeout, refused, success), created),
            () -> {
              assertTrue(created.get() == 1 ? timeout.isClosed() : refused.isClosed());
              pauses.incrementAndGet();
            })) {
      assertSame(success, actual);
      assertEquals(3, created.get());
      assertEquals(2, pauses.get());
    }
    assertTrue(timeout.isClosed());
    assertTrue(refused.isClosed());
  }

  @ParameterizedTest
  @EnumSource(ConnectionFailure.class)
  void stopsAfterThreeFailuresWithoutAFinalPause(ConnectionFailure failure) {
    List<RecordingSocket> sockets =
        List.of(
            new RecordingSocket(failure.exception()),
            new RecordingSocket(failure.exception()),
            new RecordingSocket(failure.exception()));
    AtomicInteger created = new AtomicInteger();
    AtomicInteger pauses = new AtomicInteger();

    ConnectionFailedException actual =
        assertThrows(
            ConnectionFailedException.class,
            () ->
                DeviceSimulator.tryToConnect(
                    ADDRESS,
                    factory(sockets, created),
                    () -> {
                      assertTrue(sockets.get(created.get() - 1).isClosed());
                      pauses.incrementAndGet();
                    }));

    assertEquals(3, created.get());
    assertEquals(2, pauses.get());
    assertTrue(actual.getMessage().contains("after 3 attempts"));
    assertTrue(sockets.stream().allMatch(Socket::isClosed));
  }

  @Test
  void stopsOnInterruptDuringRetryPauseAndPreservesTheFlag() throws Exception {
    // Keep the JUnit thread's interrupt status untouched, even if an assertion fails.
    isolated(
        () -> {
          RecordingSocket refused = new RecordingSocket(new ConnectException("refused"));
          AtomicInteger created = new AtomicInteger();

          ThreadInterruptedException actual =
              assertThrows(
                  ThreadInterruptedException.class,
                  () ->
                      DeviceSimulator.tryToConnect(
                          ADDRESS,
                          factory(List.of(refused), created),
                          () -> {
                            assertTrue(refused.isClosed());
                            Thread.currentThread().interrupt();
                          }));

          assertTrue(Thread.currentThread().isInterrupted());
          assertEquals(1, created.get());
          assertTrue(actual.getMessage().contains("after 1 attempts"));
          return null;
        });
  }

  @Test
  void productionPauseRestoresInterruptFlag() throws Exception {
    isolated(
        () -> {
          Thread.currentThread().interrupt();
          DeviceSimulator.sleep();

          assertTrue(Thread.currentThread().isInterrupted());
          return null;
        });
  }

  @Test
  void propagatesOtherIoFailuresAndClosesTheFailedSocketWithoutRetrying() {
    IOException failure = new SocketException("simulated non-retryable socket failure");
    RecordingSocket socket = new RecordingSocket(failure);
    AtomicInteger created = new AtomicInteger();
    AtomicInteger pauses = new AtomicInteger();

    IOException actual =
        assertThrows(
            IOException.class,
            () ->
                DeviceSimulator.tryToConnect(
                    ADDRESS, factory(List.of(socket), created), pauses::incrementAndGet));

    assertSame(failure, actual);
    assertEquals(1, created.get());
    assertEquals(0, pauses.get());
    assertTrue(socket.isClosed());
  }

  @Test
  void telemetryWriteFailureClosesTheSocketWithoutReconnecting() {
    OutputStream failingOutput =
        new OutputStream() {
          @Override
          public void write(int value) throws IOException {
            // Fresh exceptions allow close/flush failures to be suppressed normally.
            throw new IOException("simulated telemetry write failure");
          }
        };
    RecordingSocket socket = new RecordingSocket(null, failingOutput);
    AtomicInteger created = new AtomicInteger();
    AtomicInteger pauses = new AtomicInteger();

    IOException actual =
        assertThrows(
            IOException.class,
            () ->
                DeviceSimulator.run(
                    ADDRESS, factory(List.of(socket), created), pauses::incrementAndGet));

    assertEquals("simulated telemetry write failure", actual.getMessage());
    assertEquals(1, created.get());
    assertEquals(0, pauses.get());
    assertTrue(socket.isClosed());
  }

  @Test
  void failureLogsUseAttemptNumbersOneThroughThree() {
    List<String> messages = new ArrayList<>();
    Logger logger = Logger.getLogger(DeviceSimulator.class.getName());
    Handler handler =
        new Handler() {
          @Override
          public void publish(LogRecord record) {
            if (record.getMessage().startsWith("Attempt #")) {
              messages.add(record.getMessage());
            }
          }

          @Override
          public void flush() {}

          @Override
          public void close() {}
        };
    logger.addHandler(handler);
    try {
      List<RecordingSocket> sockets =
          List.of(
              new RecordingSocket(new ConnectException("refused")),
              new RecordingSocket(new ConnectException("refused")),
              new RecordingSocket(new ConnectException("refused")));
      assertThrows(
          ConnectionFailedException.class,
          () ->
              DeviceSimulator.tryToConnect(
                  ADDRESS, factory(sockets, new AtomicInteger()), () -> {}));

      assertEquals(3, messages.size());
      for (int index = 0; index < messages.size(); index++) {
        assertTrue(messages.get(index).startsWith("Attempt #" + (index + 1) + " "));
        assertTrue(messages.get(index).contains("refused"));
        assertTrue(messages.get(index).contains("127.0.0.1:19000"));
      }
    } finally {
      logger.removeHandler(handler);
      handler.close();
    }
  }

  private static Supplier<Socket> factory(List<RecordingSocket> sockets, AtomicInteger created) {
    // An unexpected extra attempt immediately fails the test instead of opening a real socket.
    return () -> sockets.get(created.getAndIncrement());
  }

  private static <T> T isolated(Callable<T> operation) throws Exception {
    ExecutorService worker =
        Executors.newSingleThreadExecutor(Thread.ofPlatform().daemon().factory());
    try {
      return worker.submit(operation).get(5, TimeUnit.SECONDS);
    } finally {
      worker.shutdownNow();
      assertTrue(worker.awaitTermination(5, TimeUnit.SECONDS));
    }
  }

  private enum ConnectionFailure {
    REFUSED,
    TIMED_OUT;

    IOException exception() {
      return this == REFUSED
          ? new ConnectException("refused")
          : new SocketTimeoutException("timed out");
    }
  }

  private static final class RecordingSocket extends Socket {
    private final IOException failure;
    private final OutputStream output;
    private SocketAddress address;
    private int connectTimeoutMillis;

    RecordingSocket(IOException failure) {
      this(failure, new ByteArrayOutputStream());
    }

    RecordingSocket(IOException failure, OutputStream output) {
      this.failure = failure;
      this.output = output;
    }

    @Override
    public void connect(SocketAddress endpoint, int timeout) throws IOException {
      address = endpoint;
      connectTimeoutMillis = timeout;
      if (failure != null) {
        throw failure;
      }
    }

    @Override
    public OutputStream getOutputStream() {
      return output;
    }
  }
}
