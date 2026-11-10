package client;

import static util.Constants.*;

import exception.ConnectionFailedException;
import exception.ThreadInterruptedException;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.logging.Logger;

public class DeviceSimulator {
  private static final Logger LOGGER = Logger.getLogger(DeviceSimulator.class.getName());
  public static final int CONNECT_RETRY_ATTEMPTS = 3;

  public static void main(String[] args) throws IOException {

    try (Socket socket = tryToConnect()) {

      try (BufferedWriter out =
          new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), CHARSET))) {

        for (int i = 0; i < 10; i++) {
          double value = Math.random() * 25;
          String line = "device_01," + i + "," + value + "\n";
          out.write(line);
          out.flush();
          sleep();

          if (Thread.currentThread().isInterrupted()) {
            break;
          }
        }
      }
    } catch (ConnectionFailedException e) {
      LOGGER.warning(e.getMessage());
    }
  }

  private static Socket tryToConnect() throws IOException {
    Socket socket = null;
    for (int i = 0; i < CONNECT_RETRY_ATTEMPTS; i++) {
      try {
        socket = new Socket();
        socket.connect(new InetSocketAddress(InetAddress.getByName(HOST), PORT), 1000);
        LOGGER.info("Successfully connected to " + HOST + ":" + PORT);
        return socket;
      } catch (ConnectException ce) {
        socket.close();
        LOGGER.warning(
            "Attempt #%d to connect to ".formatted(i)
                + HOST
                + ":"
                + PORT
                + " failed: "
                + ce.getMessage());
        if (hasRetryAttempts(i)) {
          sleep();
          if (Thread.currentThread().isInterrupted()) {
            break;
          }
        }
      }
    }

    if (Thread.currentThread().isInterrupted()) {
      throw new ThreadInterruptedException(
          "Failed to connect to "
              + HOST
              + ":"
              + PORT
              + " after %d attempts due to thread interrupted".formatted(attempt));
    }

    throw new ConnectionFailedException(
        "Failed to connect to "
            + HOST
            + ":"
            + PORT
            + " after %d attempts.".formatted(CONNECT_RETRY_ATTEMPTS));
  }

  private static boolean hasRetryAttempts(int attempt) {
    return attempt < CONNECT_RETRY_ATTEMPTS - 1;
  }

  private static void sleep() {
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      System.out.println("sleep interrupted");
      Thread.currentThread().interrupt();
    }
  }
}
