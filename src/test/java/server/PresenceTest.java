package server;

import static enums.DeviceState.OFFLINE;
import static enums.DeviceState.ONLINE;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PresenceTest {

  @ParameterizedTest
  @ValueSource(longs = {0L, -10_000_000_000L, 10_000_000_000L})
  void startsOfflineBeforeAnyMessageRegardlessOfClockOrigin(long initialNanos) {
    Presence presence = new Presence(() -> initialNanos);

    assertEquals(OFFLINE, presence.status());
  }

  @Test
  void becomesOnlineWhenFirstMessageArrivesAtTimeZero() {
    Presence presence = new Presence(() -> 0L);

    presence.markSeen();

    assertEquals(ONLINE, presence.status());
  }

  @ParameterizedTest
  @ValueSource(longs = {1L, 4_999L})
  void staysOnlineBeforeFiveSecondsHaveElapsed(long elapsedMillis) {
    AtomicLong nowNanos = new AtomicLong();
    Presence presence = new Presence(nowNanos::get);
    presence.markSeen();

    nowNanos.set(MILLISECONDS.toNanos(elapsedMillis));

    assertEquals(ONLINE, presence.status());
  }

  @ParameterizedTest
  @ValueSource(longs = {5_000L, 5_001L, 60_000L})
  void becomesOfflineAtOrAfterFiveSeconds(long elapsedMillis) {
    AtomicLong nowNanos = new AtomicLong();
    Presence presence = new Presence(nowNanos::get);
    presence.markSeen();

    nowNanos.set(MILLISECONDS.toNanos(elapsedMillis));

    assertEquals(OFFLINE, presence.status());
  }

  @ParameterizedTest
  @ValueSource(longs = {-10_000_000_000L, 10_000_000_000L})
  void measuresElapsedTimeFromTheLastMessageRatherThanTheClockOrigin(long initialNanos) {
    AtomicLong nowNanos = new AtomicLong(initialNanos);
    Presence presence = new Presence(nowNanos::get);
    presence.markSeen();

    assertEquals(ONLINE, presence.status());
    nowNanos.set(initialNanos + MILLISECONDS.toNanos(4_999));
    assertEquals(ONLINE, presence.status());
    nowNanos.set(initialNanos + MILLISECONDS.toNanos(5_000));
    assertEquals(OFFLINE, presence.status());
  }

  @Test
  void anotherMessageRestartsTheDeadlineWithoutTogglingOffline() {
    AtomicLong nowNanos = new AtomicLong();
    Presence presence = new Presence(nowNanos::get);
    presence.markSeen();

    nowNanos.set(MILLISECONDS.toNanos(4_000));
    presence.markSeen();
    assertEquals(ONLINE, presence.status());

    nowNanos.set(MILLISECONDS.toNanos(8_999));
    assertEquals(ONLINE, presence.status());
    nowNanos.set(MILLISECONDS.toNanos(9_000));
    assertEquals(OFFLINE, presence.status());
  }

  @Test
  void queryingStatusDoesNotRefreshOrRestoreActivity() {
    AtomicLong nowNanos = new AtomicLong();
    Presence presence = new Presence(nowNanos::get);
    presence.markSeen();

    nowNanos.set(MILLISECONDS.toNanos(4_000));
    assertEquals(ONLINE, presence.status());
    nowNanos.set(MILLISECONDS.toNanos(5_000));
    assertEquals(OFFLINE, presence.status());
    nowNanos.set(MILLISECONDS.toNanos(7_000));
    assertEquals(OFFLINE, presence.status());
  }

  @Test
  void messageAfterTimeoutRestoresOnlineAndStartsANewDeadline() {
    AtomicLong nowNanos = new AtomicLong();
    Presence presence = new Presence(nowNanos::get);
    presence.markSeen();
    nowNanos.set(MILLISECONDS.toNanos(5_000));
    assertEquals(OFFLINE, presence.status());

    nowNanos.set(MILLISECONDS.toNanos(10_000));
    presence.markSeen();
    assertEquals(ONLINE, presence.status());

    nowNanos.set(MILLISECONDS.toNanos(14_999));
    assertEquals(ONLINE, presence.status());
    nowNanos.set(MILLISECONDS.toNanos(15_000));
    assertEquals(OFFLINE, presence.status());
  }
}
