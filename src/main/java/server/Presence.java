package server;

import enums.DeviceState;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

public class Presence {
  private static final long WAITING_LIMIT_NANOS = TimeUnit.SECONDS.toNanos(5);
  private Long lastSeen;
  private final LongSupplier timeSupplier;

  public Presence(LongSupplier timeSupplier) {
    this.timeSupplier = timeSupplier;
  }

  public void markSeen() {
    lastSeen = timeSupplier.getAsLong();
  }

  public DeviceState status() {
    if (lastSeen == null || hasReachedLimit()) {
      return DeviceState.OFFLINE;
    }
    return DeviceState.ONLINE;
  }

  private boolean hasReachedLimit() {
    return timeSupplier.getAsLong() - lastSeen >= WAITING_LIMIT_NANOS;
  }
}
