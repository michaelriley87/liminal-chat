package com.michaelriley87.liminal_chat;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Service;

@Service
public class RoomService {

  public enum AddChatterResult {
    ADDED,
    ROOM_UNAVAILABLE,
    NAME_TAKEN
  }

  private final Map<String, Room> rooms = new ConcurrentHashMap<>();
  private final Map<String, List<Chatter>> chattersByRoom = new ConcurrentHashMap<>();

  public Room createRoom() {
    String code = generateRoomCode();
    Room room = new Room(code);

    rooms.put(code, room);
    chattersByRoom.put(code, new CopyOnWriteArrayList<>());

    return room;
  }

  public Room getRoom(String code) {
    return rooms.get(code);
  }

  public AddChatterResult tryAddChatter(String roomCode, Chatter chatter) {
    List<Chatter> chatters = chattersByRoom.get(roomCode);

    if (chatters == null || rooms.get(roomCode) == null) {
      return AddChatterResult.ROOM_UNAVAILABLE;
    }

    synchronized (chatters) {
      if (rooms.get(roomCode) == null) {
        return AddChatterResult.ROOM_UNAVAILABLE;
      }

      boolean nameTaken =
          chatters.stream()
              .anyMatch(
                  existingChatter -> existingChatter.getName().equalsIgnoreCase(chatter.getName()));

      if (nameTaken) {
        return AddChatterResult.NAME_TAKEN;
      }

      chatters.add(chatter);
      return AddChatterResult.ADDED;
    }
  }

  public void removeChatter(String roomCode, String name) {
    List<Chatter> chatters = chattersByRoom.get(roomCode);

    if (chatters == null) {
      return;
    }

    for (Chatter chatter : chatters) {
      if (chatter.getName().equals(name)) {
        chatters.remove(chatter);
        return;
      }
    }
  }

  public List<String> getChatterNames(String roomCode) {
    List<Chatter> chatters = chattersByRoom.get(roomCode);

    if (chatters == null) {
      return List.of();
    }

    return chatters.stream().map(chatter -> chatter.getName()).toList();
  }

  public void updateRoomActivity(String roomCode) {
    Room room = rooms.get(roomCode);

    if (room != null) {
      room.updateLastActivity();
    }
  }

  public List<String> removeExpiredRooms(Duration inactivityLimit) {
    Instant expiryCutoff = Instant.now().minus(inactivityLimit);
    List<String> expiredRoomCodes = new ArrayList<>();

    rooms.forEach(
        (roomCode, room) -> {
          if (room.getLastActivity().isBefore(expiryCutoff)) {
            boolean removed = rooms.remove(roomCode, room);

            if (removed) {
              chattersByRoom.remove(roomCode);
              expiredRoomCodes.add(roomCode);

              System.out.println("Expired room " + roomCode);
            }
          }
        });

    return expiredRoomCodes;
  }

  private String generateRoomCode() {
    return UUID.randomUUID().toString().substring(0, 5).toUpperCase();
  }
}
