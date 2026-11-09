package server;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static util.Constants.CHARSET;
import static util.Constants.HOST;
import static util.Constants.MAX_MESSAGE_LENGTH;

import exception.IncompleteLineException;
import exception.MaxLengthExceededException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(15)
class TcpServerIntegrationTest {

  @Test
  void receivesTenMessagesSentInASingleWriteAndClosesAfterEof() throws Exception {
    withSession(
        2000,
        (client, completion, received) -> {
          List<String> expected = new ArrayList<>();
          for (int sequence = 0; sequence < 10; sequence++) {
            expected.add("device_01," + sequence + ",23.5");
          }

          send(client, String.join("\n", expected) + "\n");
          client.shutdownOutput();

          completion.get(5, SECONDS);
          assertEquals(expected, new ArrayList<>(received));
          assertPeerClosed(client);
        });
  }

  @Test
  void receivesUtf8MessageWrittenInFragmentsBeforeEof() throws Exception {
    withSession(
        2000,
        (client, completion, received) -> {
          String message = "пристрій_01,0,23.5";
          byte[] bytes = (message + "\n").getBytes(CHARSET);
          OutputStream output = client.getOutputStream();
          // The split falls inside the first UTF-8 character. TCP may still coalesce the writes.
          output.write(bytes, 0, 1);
          output.flush();
          output.write(bytes, 1, bytes.length - 1);
          output.flush();

          assertEquals(message, received.poll(5, SECONDS));
          client.shutdownOutput();
          completion.get(5, SECONDS);
          assertTrue(received.isEmpty());
          assertPeerClosed(client);
        });
  }

  @Test
  void accepts256CharactersAndADelimiter() throws Exception {
    withSession(
        2000,
        (client, completion, received) -> {
          String message = "x".repeat(MAX_MESSAGE_LENGTH);
          send(client, message + "\n");
          client.shutdownOutput();

          completion.get(5, SECONDS);
          assertEquals(List.of(message), new ArrayList<>(received));
          assertPeerClosed(client);
        });
  }

  @Test
  void rejects257CharactersWithoutWaitingForDelimiterOrClientEof() throws Exception {
    withSession(
        2000,
        (client, completion, received) -> {
          send(client, "x".repeat(MAX_MESSAGE_LENGTH + 1));

          assertSessionFailure(MaxLengthExceededException.class, completion);
          assertTrue(received.isEmpty());
          assertPeerClosed(client);
        });
  }

  @Test
  void rejectsPartialMessageAtEofButPreservesEarlierCompletedMessages() throws Exception {
    withSession(
        2000,
        (client, completion, received) -> {
          send(client, "complete\npartial");
          client.shutdownOutput();

          assertSessionFailure(IncompleteLineException.class, completion);
          assertEquals(List.of("complete"), new ArrayList<>(received));
          assertPeerClosed(client);
        });
  }

  @Test
  void acceptsAnEmptyConnectionClosedByTheClient() throws Exception {
    withSession(
        2000,
        (client, completion, received) -> {
          client.shutdownOutput();

          completion.get(5, SECONDS);
          assertTrue(received.isEmpty());
          assertPeerClosed(client);
        });
  }

  @Test
  void timesOutAndClosesAConnectionThatSendsNoData() throws Exception {
    withSession(
        500,
        (client, completion, received) -> {
          assertSessionFailure(SocketTimeoutException.class, completion);
          assertTrue(received.isEmpty());
          assertPeerClosed(client);
        });
  }

  @Test
  void timesOutRatherThanDeliveringAPartialMessageOnAnOpenConnection() throws Exception {
    withSession(
        500,
        (client, completion, received) -> {
          send(client, "partial");

          assertSessionFailure(SocketTimeoutException.class, completion);
          assertTrue(received.isEmpty());
          assertPeerClosed(client);
        });
  }

  private static void send(Socket client, String data) throws IOException {
    client.getOutputStream().write(data.getBytes(CHARSET));
    client.getOutputStream().flush();
  }

  private static void assertPeerClosed(Socket client) throws IOException {
    assertEquals(-1, client.getInputStream().read(), "Server must close the accepted socket");
  }

  private static void assertSessionFailure(Class<? extends Throwable> type, Future<?> completion) {
    ExecutionException failure =
        assertThrows(ExecutionException.class, () -> completion.get(5, SECONDS));
    assertInstanceOf(type, failure.getCause());
  }

  private static void withSession(int readTimeoutMillis, SessionAssertions assertions)
      throws Exception {
    ExecutorService worker =
        Executors.newSingleThreadExecutor(Thread.ofPlatform().daemon().factory());
    AtomicReference<Socket> acceptedSocket = new AtomicReference<>();
    BlockingQueue<String> received = new LinkedBlockingQueue<>();
    try (ServerSocket listener = new ServerSocket(0, 1, InetAddress.getByName(HOST));
        Socket client = new Socket()) {
      // Binding completes before the client connects; no readiness sleeps or fixed ports.
      listener.setSoTimeout(5000);
      Future<?> completion =
          worker.submit(
              () -> {
                Socket accepted = listener.accept();
                acceptedSocket.set(accepted);
                TcpServer.handleConnection(accepted, readTimeoutMillis, received::add);
                return null;
              });
      client.connect(new InetSocketAddress(HOST, listener.getLocalPort()), 5000);
      client.setSoTimeout(5000);

      assertions.verify(client, completion, received);
    } finally {
      try {
        Socket accepted = acceptedSocket.get();
        if (accepted != null) {
          accepted.close();
        }
      } finally {
        worker.shutdownNow();
        assertTrue(worker.awaitTermination(5, SECONDS), "Server worker must terminate");
      }
    }
  }

  @FunctionalInterface
  private interface SessionAssertions {
    void verify(Socket client, Future<?> completion, BlockingQueue<String> received)
        throws Exception;
  }
}
