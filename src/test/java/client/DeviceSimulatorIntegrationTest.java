package client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static util.Constants.CHARSET;
import static util.Constants.HOST;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(10)
class DeviceSimulatorIntegrationTest {

  @Test
  void connectsSendsTenTelemetryMessagesAndClosesTheConnection() throws Exception {
    try (ServerSocket listener = new ServerSocket(0, 1, InetAddress.getByName(HOST))) {
      listener.setSoTimeout(5000);
      InetSocketAddress address = new InetSocketAddress(HOST, listener.getLocalPort());

      // The small telemetry batch fits in the socket buffers; no background server is needed.
      DeviceSimulator.run(address, Socket::new, () -> {});

      try (Socket accepted = listener.accept()) {
        accepted.setSoTimeout(5000);
        try (BufferedReader reader =
            new BufferedReader(new InputStreamReader(accepted.getInputStream(), CHARSET))) {
          for (int sequence = 0; sequence < 10; sequence++) {
            String line = reader.readLine();
            assertTrue(line != null, "Expected telemetry message " + sequence);
            String[] fields = line.split(",");
            assertEquals(3, fields.length);
            assertEquals("device_01", fields[0]);
            assertEquals(Integer.toString(sequence), fields[1]);
            double temperature = Double.parseDouble(fields[2]);
            assertTrue(temperature >= 0 && temperature < 25);
          }
          assertNull(reader.readLine(), "The client must close after sending ten messages");
        }
      }
    }
  }
}
