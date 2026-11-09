package client;

import static util.Constants.*;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;

public class DeviceSimulator {

  public static void main(String[] args) throws IOException {
    try (Socket socket = new Socket(HOST, PORT);
        BufferedWriter out =
            new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), CHARSET))) {

      for (int i = 0; i < 10; i++) {
        double value = Math.random() * 25;
        String line = "device_01," + i + "," + value + "\n";
        out.write(line);
        out.flush();
        sleep();
      }
    }
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
