package server;

import static util.Constants.*;

import exception.MaxLengthExceededException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.logging.Logger;

public class TcpServer {

  private static Logger LOGGER = Logger.getLogger(TcpServer.class.getName());

  public static void main(String[] args) throws IOException {
    try (ServerSocket serverSocket = new ServerSocket(PORT, 1, InetAddress.getByName(HOST))) {
      readFromSocket(serverSocket);
    }
  }

  private static void readFromSocket(ServerSocket serverSocket) throws IOException {
    try (Socket socket = serverSocket.accept()) {
      socket.setSoTimeout(5000);
      try (BufferedReader bufferedReader =
          new BufferedReader(new InputStreamReader(socket.getInputStream(), CHARSET))) {

        StringBuilder stringBuilder = new StringBuilder();
        int c;
        while ((c = bufferedReader.read()) != -1) {
          char character = (char) c;
          stringBuilder.append(character);
          if (character == '\n') {
            System.out.print(stringBuilder);
            stringBuilder.setLength(0);
          }
          if (character != '\n' && stringBuilder.length() >= MAX_MESSAGE_LENGTH) {
            throw new MaxLengthExceededException(
                "message exceeded max length of " + MAX_MESSAGE_LENGTH + " characters");
          }
        }
      }
    } catch (MaxLengthExceededException maxLengthExceededException) {
      LOGGER.warning(maxLengthExceededException.getMessage());
    } catch (SocketTimeoutException socketTimeoutException) {
      LOGGER.warning("Socket timeout exception: " + socketTimeoutException.getMessage());
    }
  }
}
