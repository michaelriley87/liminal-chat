package com.michaelriley87.liminal_chat;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final RoomService roomService;

    public ChatWebSocketHandler(RoomService roomService) {
        this.roomService = roomService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        if (session.getUri() == null) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        var parameters = UriComponentsBuilder
            .fromUri(session.getUri())
            .build()
            .getQueryParams();
        
        String roomCode = parameters.getFirst("room");
        String name = parameters.getFirst("name");

        if (roomCode == null || name == null || roomService.getRoom(roomCode) == null) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        session.getAttributes().put("roomCode", roomCode);
        session.getAttributes().put("name", name);

        roomService.addChatter(roomCode, new Chatter(name));

        System.out.println(name + " connected to room " + roomCode);
    }

    @Override
    public void afterConnectionClosed(
            WebSocketSession session,
            CloseStatus status) {

        String roomCode =
            (String) session.getAttributes().get("roomCode");

        String name =
            (String) session.getAttributes().get("name");

        if (roomCode != null && name != null) {
            roomService.removeChatter(roomCode, name);
            System.out.println(name + " disconnected from room " + roomCode);
        }
    }
}
