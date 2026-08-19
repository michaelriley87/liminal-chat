package com.michaelriley87.liminal_chat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;

@Service
public class RoomService {

  public enum AddChatterResult {
    ADDED,
    ROOM_UNAVAILABLE,
    NAME_TAKEN
  }

  private final Map<String, RoomState> rooms = new ConcurrentHashMap<>();
  private final Clock clock;
  private final Supplier<String> roomCodeSupplier;

  public RoomService() {
    this(Clock.systemUTC(), RoomService::generateRoomCode);
  }

  RoomService(Clock clock, Supplier<String> roomCodeSupplier) {
    this.clock = clock;
    this.roomCodeSupplier = roomCodeSupplier;
  }

  public Room createRoom() {
    while (true) {
      String code = roomCodeSupplier.get();
      Room room = new Room(code, clock);
      RoomState roomState = new RoomState(room);

      if (rooms.putIfAbsent(code, roomState) == null) {
        return room;
      }
    }
  }

  public Room getRoom(String code) {
    RoomState roomState = rooms.get(code);
    return roomState == null ? null : roomState.room;
  }

  public AddChatterResult tryAddChatter(String roomCode, Chatter chatter) {
    RoomState roomState = rooms.get(roomCode);

    if (roomState == null) {
      return AddChatterResult.ROOM_UNAVAILABLE;
    }

    synchronized (roomState) {
      if (rooms.get(roomCode) != roomState) {
        return AddChatterResult.ROOM_UNAVAILABLE;
      }

      boolean nameTaken =
          roomState.chatters.stream()
              .anyMatch(
                  existingChatter -> existingChatter.getName().equalsIgnoreCase(chatter.getName()));

      if (nameTaken) {
        return AddChatterResult.NAME_TAKEN;
      }

      roomState.chatters.add(chatter);
      roomState.room.updateLastActivity();
      return AddChatterResult.ADDED;
    }
  }

  public void removeChatter(String roomCode, String name) {
    RoomState roomState = rooms.get(roomCode);

    if (roomState == null) {
      return;
    }

    synchronized (roomState) {
      roomState.chatters.removeIf(chatter -> chatter.getName().equals(name));
    }
  }

  public List<String> getChatterNames(String roomCode) {
    RoomState roomState = rooms.get(roomCode);

    if (roomState == null) {
      return List.of();
    }

    synchronized (roomState) {
      return roomState.chatters.stream().map(chatter -> chatter.getName()).toList();
    }
  }

  public void updateRoomActivity(String roomCode) {
    RoomState roomState = rooms.get(roomCode);

    if (roomState != null) {
      synchronized (roomState) {
        if (rooms.get(roomCode) == roomState) {
          roomState.room.updateLastActivity();
        }
      }
    }
  }

  public List<String> removeExpiredRooms(Duration inactivityLimit) {
    Instant expiryCutoff = clock.instant().minus(inactivityLimit);
    List<String> expiredRoomCodes = new ArrayList<>();

    rooms.forEach(
        (roomCode, roomState) -> {
          synchronized (roomState) {
            if (roomState.room.getLastActivity().isBefore(expiryCutoff)
                && rooms.remove(roomCode, roomState)) {
              expiredRoomCodes.add(roomCode);
              System.out.println("Expired room " + roomCode);
            }
          }
        });

    return expiredRoomCodes;
  }

  private static String generateRoomCode() {
    return UUID.randomUUID().toString().substring(0, 5).toUpperCase();
  }

  private static class RoomState {
    private final Room room;
    private final List<Chatter> chatters = new ArrayList<>();

    private RoomState(Room room) {
      this.room = room;
    }
  }
}
