package com.michaelriley87.liminal_chat;

import java.time.Duration;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RoomExpiryScheduler {

  private static final Duration INACTIVITY_LIMIT = Duration.ofHours(1);

  private final RoomService roomService;
  private final ChatWebSocketHandler chatWebSocketHandler;

  public RoomExpiryScheduler(RoomService roomService, ChatWebSocketHandler chatWebSocketHandler) {

    this.roomService = roomService;
    this.chatWebSocketHandler = chatWebSocketHandler;
  }

  @Scheduled(fixedRate = 60_000)
  public void removeExpiredRooms() {
    List<String> expiredRoomCodes = roomService.removeExpiredRooms(INACTIVITY_LIMIT);

    for (String roomCode : expiredRoomCodes) {
      chatWebSocketHandler.closeRoomSessions(roomCode);
    }
  }
}
