package com.michaelriley87.liminal_chat;

import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Service;

@Service
public class RoomService {

    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final Map<String, List<Chatter>> chattersByRoom = new ConcurrentHashMap<>();

    public Room createRoom(String chatterName) {
        String code = generateRoomCode();
        Room room = new Room(code);
        List<Chatter> chatters = new CopyOnWriteArrayList<>();
        chatters.add(new Chatter(chatterName));
        rooms.put(code, room);
        chattersByRoom.put(code, chatters);
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

    public void addChatter(String roomCode, Chatter chatter) {
        List<Chatter> chatters = chattersByRoom.get(roomCode);
        if (chatters != null) {
            chatters.add(chatter);
        }
    }

    public void removeChatter(String roomCode, String name) {
        List<Chatter> chatters = chattersByRoom.get(roomCode);
        if (chatters != null) {
            chatters.removeIf(chatter -> chatter.getName().equals(name));
        }
    }
}
