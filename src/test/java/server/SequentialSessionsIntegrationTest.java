package server;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static util.Constants.CHARSET;
import static util.Constants.MAX_MESSAGE_LENGTH;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@Timeout(15)
class SequentialSessionsIntegrationTest {

  @ParameterizedTest
  @EnumSource(SessionEnd.class)
  void acceptsAnotherClientOnTheSameListenerAfter(SessionEnd ending) throws Exception {
    ExecutorService worker =
        Executors.newSingleThreadExecutor(Thread.ofPlatform().daemon().factory());
    BlockingQueue<String> received = new LinkedBlockingQueue<>();

    try (ServerSocket listener = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
      InetSocketAddress address =
          new InetSocketAddress(listener.getInetAddress(), listener.getLocalPort());
      // For the protocol cases, a timeout must not hide a missing rejection.
      int readTimeoutMillis = ending == SessionEnd.READ_TIMEOUT ? 1000 : 0;
      Future<?> serving =
          worker.submit(
              () -> {
                try {
                  TcpServer.serve(listener, readTimeoutMillis, received::add);
                } catch (SocketException e) {
                  if (!listener.isClosed()) {
                    throw e;
                  }
                }
                return null;
              });

      try {
        try (Socket first = connect(address)) {
          finishFirstSession(first, ending);
          assertEquals(-1, first.getInputStream().read(), "The first session must close");
        }

        try (Socket second = connect(address)) {
          send(second, "second\n");
          second.shutdownOutput();
          assertEquals(-1, second.getInputStream().read(), "The second session must close");
        }

        List<String> expected =
            ending == SessionEnd.CLEAN_EOF ? List.of("first", "second") : List.of("second");
        assertEquals(expected, List.copyOf(received));
      } finally {
        listener.close();
        serving.get(5, SECONDS);
      }
    } finally {
      worker.shutdownNow();
      assertTrue(worker.awaitTermination(5, SECONDS), "The server worker must terminate");
    }
  }

  private static void finishFirstSession(Socket client, SessionEnd ending) throws IOException {
    switch (ending) {
      case CLEAN_EOF -> {
        send(client, "first\n");
        client.shutdownOutput();
      }
      case READ_TIMEOUT -> {
        // Keep the client open and silent until the server closes the session.
      }
      case MESSAGE_TOO_LONG -> send(client, "x".repeat(MAX_MESSAGE_LENGTH + 1));
      case PARTIAL_EOF -> {
        send(client, "partial");
        client.shutdownOutput();
      }
    }
  }

  private static Socket connect(InetSocketAddress address) throws IOException {
    Socket client = new Socket();
    try {
      client.connect(address, 5000);
      client.setSoTimeout(5000);
      return client;
    } catch (IOException failure) {
      try {
        client.close();
      } catch (IOException closeFailure) {
        failure.addSuppressed(closeFailure);
      }
      throw failure;
    }
  }

  private static void send(Socket client, String data) throws IOException {
    client.getOutputStream().write(data.getBytes(CHARSET));
  }

  private enum SessionEnd {
    CLEAN_EOF,
    READ_TIMEOUT,
    MESSAGE_TOO_LONG,
    PARTIAL_EOF
  }
}
