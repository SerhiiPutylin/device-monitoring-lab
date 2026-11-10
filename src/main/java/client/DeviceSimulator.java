package client;

import static util.Constants.*;

import exception.ConnectionFailedException;
import exception.ThreadInterruptedException;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.*;
import java.util.function.Supplier;
import java.util.logging.Logger;

public class DeviceSimulator {
  private static final Logger LOGGER = Logger.getLogger(DeviceSimulator.class.getName());
  public static final int CONNECT_RETRY_ATTEMPTS = 3;

  public static void main(String[] args) throws IOException {
    run(
        new InetSocketAddress(InetAddress.getByName(HOST), PORT),
        Socket::new,
        DeviceSimulator::sleep);
  }

  // Production uses real sockets and one-second pauses; tests supply controlled collaborators.
  static void run(InetSocketAddress address, Supplier<Socket> sockets, Runnable pause)
      throws IOException {
    try (Socket socket = tryToConnect(address, sockets, pause)) {

      try (BufferedWriter out =
          new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), CHARSET))) {

        for (int i = 0; i < 10; i++) {
          double value = Math.random() * 25;
          String line = "device_01," + i + "," + value + "\n";
          out.write(line);
          out.flush();
          pause.run();

          if (Thread.currentThread().isInterrupted()) {
            break;
          }
        }
      }
    } catch (ConnectionFailedException e) {
      LOGGER.warning(e.getMessage());
    }
  }

  static Socket tryToConnect(InetSocketAddress address, Supplier<Socket> sockets, Runnable pause)
      throws IOException {
    String destination = address.getHostString() + ":" + address.getPort();
    int attempt = 0;
    while (attempt < CONNECT_RETRY_ATTEMPTS) {
      try {
        Socket socket = connectOnce(address, sockets);
        LOGGER.info("Successfully connected to " + destination);
        return socket;
      } catch (ConnectException | SocketTimeoutException e) {
        attempt++;
        LOGGER.warning(
            "Attempt #%d to connect to ".formatted(attempt)
                + destination
                + " failed: "
                + e.getMessage());
        if (hasRetryAttempts(attempt)) {
          pause.run();
          if (Thread.currentThread().isInterrupted()) {
            break;
          }
        }
      }
    }

    if (Thread.currentThread().isInterrupted()) {
      throw new ThreadInterruptedException(
          "Failed to connect to "
              + destination
              + " after %d attempts due to thread interrupted".formatted(attempt));
    }

    throw new ConnectionFailedException(
        "Failed to connect to "
            + destination
            + " after %d attempts.".formatted(CONNECT_RETRY_ATTEMPTS));
  }

  private static Socket connectOnce(InetSocketAddress address, Supplier<Socket> sockets)
      throws IOException {
    Socket socket = sockets.get();
    try {
      socket.connect(address, 1000);
      return socket;
    } catch (IOException | RuntimeException failure) {
      try {
        socket.close();
      } catch (IOException closeFailure) {
        failure.addSuppressed(closeFailure);
      }
      throw failure;
    }
  }

  private static boolean hasRetryAttempts(int attempt) {
    return attempt < CONNECT_RETRY_ATTEMPTS;
  }

  static void sleep() {
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      System.out.println("sleep interrupted");
      Thread.currentThread().interrupt();
    }
  }
}
