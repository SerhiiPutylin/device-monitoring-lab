package util;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class Constants {
  public static final String HOST = "127.0.0.1";
  public static final int PORT = 9000;
  public static final Charset CHARSET = StandardCharsets.UTF_8;

  public static final int MAX_MESSAGE_LENGTH = 256;

  private Constants() {}
}
