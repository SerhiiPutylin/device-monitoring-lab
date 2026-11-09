package server;

import static util.Constants.*;

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

        String line;
        while ((line = bufferedReader.readLine()) != null) {
          System.out.println(line);
        }
      }
    } catch (SocketTimeoutException socketTimeoutException) {
      LOGGER.warning("Socket timeout exception: " + socketTimeoutException.getMessage());
    }
  }
}
