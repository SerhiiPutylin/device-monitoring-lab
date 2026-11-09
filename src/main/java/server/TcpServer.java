package server;

import static util.Constants.CHARSET;
import static util.Constants.PORT;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class TcpServer {

  public static void main(String[] args) throws IOException {
    try (ServerSocket serverSocket = new ServerSocket(PORT)) {

      try (Socket socket = serverSocket.accept();
          BufferedReader bufferedReader =
              new BufferedReader(new InputStreamReader(socket.getInputStream(), CHARSET))) {

        String line;
        while ((line = bufferedReader.readLine()) != null) {
          System.out.println(line);
        }
      }
    }
  }
}
