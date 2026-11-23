package server;

import enums.ServerState;
import java.util.function.LongSupplier;

public class Presence {
  private static final long WAITING_LIMIT_NANOS = 5_000_000;
  private ServerState state = ServerState.OFFLINE;
  private long lastSeen;
  private final LongSupplier timeSupplier;

  public Presence(LongSupplier timeSupplier) {
    this.timeSupplier = timeSupplier;
  }

  public void markSeen() {
    lastSeen = timeSupplier.getAsLong();
    state = ServerState.ONLINE;
  }

  public ServerState status() {
    if (hasReachedLimit()) {
      return ServerState.OFFLINE;
    }
    return state;
  }

  private boolean hasReachedLimit() {
    return timeSupplier.getAsLong() - lastSeen >= WAITING_LIMIT_NANOS;
  }
}
