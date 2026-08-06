package com.michaelriley87.liminal_chat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class RoomExpirySchedulerTest {

  @Test
  void closesWebSocketSessionsForExpiredRooms() {
    RoomService roomService = mock(RoomService.class);
    ChatWebSocketHandler chatWebSocketHandler = mock(ChatWebSocketHandler.class);
    RoomExpiryScheduler scheduler = new RoomExpiryScheduler(roomService, chatWebSocketHandler);

    when(roomService.removeExpiredRooms(Duration.ofHours(1))).thenReturn(List.of("ABCDE", "12345"));

    scheduler.removeExpiredRooms();

    verify(chatWebSocketHandler).closeRoomSessions("ABCDE");
    verify(chatWebSocketHandler).closeRoomSessions("12345");
  }
}
