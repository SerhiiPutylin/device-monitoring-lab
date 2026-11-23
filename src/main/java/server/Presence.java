package server;

import enums.DeviceState;
import java.util.function.LongSupplier;

public class Presence {
  private static final long WAITING_LIMIT_NANOS = 5_000_000;
  private long lastSeen;
  private final LongSupplier timeSupplier;

  public Presence(LongSupplier timeSupplier) {
    this.timeSupplier = timeSupplier;
  }

  public void markSeen() {
    lastSeen = timeSupplier.getAsLong();
  }

  public DeviceState status() {
    if (hasReachedLimit()) {
      return DeviceState.OFFLINE;
    }
    return DeviceState.ONLINE;
  }

  private boolean hasReachedLimit() {
    return timeSupplier.getAsLong() - lastSeen >= WAITING_LIMIT_NANOS;
  }
}
