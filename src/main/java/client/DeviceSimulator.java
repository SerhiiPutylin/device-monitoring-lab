package client;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;

public class DeviceSimulator {

  public static void main(String[] args) throws IOException {
    try (Socket socket = new Socket("127.0.0.1", 9000)) {

      BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());

      for (int i = 0; i < 10; i++) {
        double value = Math.random() * 25;
        String line = "device_01," + i + "," + value + "\n";
        out.write(line.getBytes());
        out.flush();
        Thread.sleep(1000);
      }
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
