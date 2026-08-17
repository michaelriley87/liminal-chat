package com.michaelriley87.liminal_chat;

import java.time.Clock;
import java.time.Instant;

public class Room {
  private final String code;
  private final Clock clock;
  private volatile Instant lastActivity;

  public Room(String code) {
    this(code, Clock.systemUTC());
  }

  Room(String code, Clock clock) {
    this.code = code;
    this.clock = clock;
    this.lastActivity = clock.instant();
  }

  public String getCode() {
    return code;
  }

  public Instant getLastActivity() {
    return lastActivity;
  }

  public void updateLastActivity() {
    lastActivity = clock.instant();
  }
}
