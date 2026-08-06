package com.michaelriley87.liminal_chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RoomServiceTest {

  private RoomService roomService;

  @BeforeEach
  void setUp() {
    roomService = new RoomService();
  }

  @Test
  void addsChatterToExistingRoom() {
    Room room = roomService.createRoom();

    RoomService.AddChatterResult result =
        roomService.tryAddChatter(room.getCode(), new Chatter("Michael"));

    assertEquals(RoomService.AddChatterResult.ADDED, result);
  }

  @Test
  void rejectsDuplicateName() {
    Room room = roomService.createRoom();

    roomService.tryAddChatter(room.getCode(), new Chatter("Michael"));

    RoomService.AddChatterResult result =
        roomService.tryAddChatter(room.getCode(), new Chatter("Michael"));

    assertEquals(RoomService.AddChatterResult.NAME_TAKEN, result);
  }

  @Test
  void rejectsDuplicateNameIgnoringCapitalisation() {
    Room room = roomService.createRoom();

    roomService.tryAddChatter(room.getCode(), new Chatter("Michael"));

    RoomService.AddChatterResult result =
        roomService.tryAddChatter(room.getCode(), new Chatter("michael"));

    assertEquals(RoomService.AddChatterResult.NAME_TAKEN, result);
  }

  @Test
  void allowsSameNameInDifferentRooms() {
    Room firstRoom = roomService.createRoom();
    Room secondRoom = roomService.createRoom();

    roomService.tryAddChatter(firstRoom.getCode(), new Chatter("Michael"));

    RoomService.AddChatterResult result =
        roomService.tryAddChatter(secondRoom.getCode(), new Chatter("Michael"));

    assertEquals(RoomService.AddChatterResult.ADDED, result);
  }

  @Test
  void rejectsChatterForMissingRoom() {
    RoomService.AddChatterResult result =
        roomService.tryAddChatter("ABCDE", new Chatter("Michael"));

    assertEquals(RoomService.AddChatterResult.ROOM_UNAVAILABLE, result);
  }

  @Test
  void removesChatterFromRoom() {
    Room room = roomService.createRoom();
    Chatter chatter = new Chatter("Michael");

    roomService.tryAddChatter(room.getCode(), chatter);
    roomService.removeChatter(room.getCode(), chatter.getName());

    assertTrue(roomService.getChatterNames(room.getCode()).isEmpty());
  }

  @Test
  void returnsEmptyParticipantListForMissingRoom() {
    assertTrue(roomService.getChatterNames("ABCDE").isEmpty());
  }

  @Test
  void keepsActiveRooms() {
    Room room = roomService.createRoom();

    List<String> removedRooms = roomService.removeExpiredRooms(Duration.ofHours(1));

    assertTrue(removedRooms.isEmpty());
    assertEquals(room, roomService.getRoom(room.getCode()));
  }

  @Test
  void removesExpiredRoomsAndTheirParticipants() {
    Room room = roomService.createRoom();
    roomService.tryAddChatter(room.getCode(), new Chatter("Michael"));

    List<String> removedRooms = roomService.removeExpiredRooms(Duration.ofSeconds(-1));

    assertEquals(List.of(room.getCode()), removedRooms);
    assertNull(roomService.getRoom(room.getCode()));
    assertTrue(roomService.getChatterNames(room.getCode()).isEmpty());
  }
}
