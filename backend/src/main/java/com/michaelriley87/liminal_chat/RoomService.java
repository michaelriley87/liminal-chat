package com.michaelriley87.liminal_chat;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class RoomService {

    private final Map<String, Room> rooms = new ConcurrentHashMap<>();

    public Room createRoom() {
        String code = generateRoomCode();
        Room room = new Room(code);

        rooms.put(code, room);

        return room;
    }

    public Room getRoom(String code) {
        return rooms.get(code);
    }

    private String generateRoomCode() {
        return UUID.randomUUID()
            .toString()
            .substring(0, 5)
            .toUpperCase();
    }
}
