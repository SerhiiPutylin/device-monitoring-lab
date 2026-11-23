package server;

import enums.ServerState;
import java.time.LocalDateTime;

public class Presence {
  private static final int WAITING_LIMIT = 5;
  private ServerState state = ServerState.OFFLINE;
  private LocalDateTime lastSeen;

  public void markSeen() {
    lastSeen = LocalDateTime.now();
    state = ServerState.ONLINE;
  }

  public ServerState status() {
    if (hasReachedLimit()) {
      return ServerState.OFFLINE;
    }
    return state;
  }

  private boolean hasReachedLimit() {
    return lastSeen.isBefore(LocalDateTime.now().minusSeconds(WAITING_LIMIT));
  }
}
