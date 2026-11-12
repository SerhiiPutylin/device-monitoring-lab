package server;

import static util.Constants.*;

import exception.IncompleteLineException;
import exception.MaxLengthExceededException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class TcpServer {

  private static final Logger LOGGER = Logger.getLogger(TcpServer.class.getName());

  public static void main(String[] args) throws IOException {
    try (ServerSocket serverSocket = new ServerSocket(PORT, 1, InetAddress.getByName(HOST))) {
      serve(serverSocket, 5000, System.out::println);
    }
  }

  // The caller owns the listener; each accepted socket belongs to handleConnection.
  static void serve(ServerSocket listener, int readTimeoutMillis, Consumer<String> onMessage)
      throws IOException {
    while (true) {
      Socket socket = listener.accept();
      try {
        handleConnection(socket, readTimeoutMillis, onMessage);
      } catch (MaxLengthExceededException | IncompleteLineException e) {
        LOGGER.warning(e.getMessage());
      } catch (SocketTimeoutException e) {
        LOGGER.warning("Socket timeout exception: " + e.getMessage());
      }
    }
  }

  // Owns and closes the accepted socket, including when reading or processing fails.
  static void handleConnection(
      Socket socket, int readTimeoutMillis, Consumer<String> onMessage) throws IOException {
    try (socket) {
      socket.setSoTimeout(readTimeoutMillis);
      try (BufferedReader reader =
          new BufferedReader(new InputStreamReader(socket.getInputStream(), CHARSET))) {
        NewlineMessageReader messages = new NewlineMessageReader(reader, MAX_MESSAGE_LENGTH);
        String message;
        while ((message = messages.readMessage()) != null) {
          onMessage.accept(message);
        }
      }
    }
  }
}
