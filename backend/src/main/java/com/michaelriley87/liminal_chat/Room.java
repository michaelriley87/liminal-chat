package com.michaelriley87.liminal_chat;

import java.time.Instant;

public class Room {
  private final String code;
  private Instant lastActivity;

  public Room(String code) {
    this.code = code;
    this.lastActivity = Instant.now();
  }

  public String getCode() {
    return code;
  }

  public Instant getLastActivity() {
    return lastActivity;
  }

  public void updateLastActivity() {
    lastActivity = Instant.now();
  }
}
