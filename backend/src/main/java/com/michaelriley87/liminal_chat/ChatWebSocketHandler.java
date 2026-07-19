package com.michaelriley87.liminal_chat;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import tools.jackson.databind.ObjectMapper;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final RoomService roomService;
    private final ObjectMapper objectMapper;

    private final Map<String, List<WebSocketSession>> sessionsByRoom = new ConcurrentHashMap<>();

    public ChatWebSocketHandler(RoomService roomService, ObjectMapper objectMapper) {
        this.roomService = roomService;
        this.objectMapper = objectMapper;
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

        sessionsByRoom.computeIfAbsent(roomCode, code -> new CopyOnWriteArrayList<>()).add(session);

        roomService.addChatter(roomCode, new Chatter(name));

        System.out.println(name + " connected to room " + roomCode);
    }

    @Override
    protected void handleTextMessage(
            WebSocketSession senderSession,
            TextMessage message) throws Exception {

        String roomCode =
            (String) senderSession.getAttributes().get("roomCode");

        String name =
            (String) senderSession.getAttributes().get("name");

        if (roomCode == null || name == null) {
            return;
        }

        ChatMessage incomingMessage =
            objectMapper.readValue(
                message.getPayload(),
                ChatMessage.class
            );

        if (incomingMessage.getContent() == null
                || incomingMessage.getContent().isBlank()) {
            return;
        }

        ChatMessage outgoingMessage = new ChatMessage(
            "CHAT_MESSAGE",
            name,
            incomingMessage.getContent()
        );

        String outgoingJson =
            objectMapper.writeValueAsString(outgoingMessage);

        List<WebSocketSession> roomSessions =
            sessionsByRoom.get(roomCode);

        if (roomSessions == null) {
            return;
        }

        for (WebSocketSession session : roomSessions) {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(outgoingJson));
            }
        }
    }

    @Override
    public void afterConnectionClosed(
            WebSocketSession session,
            CloseStatus status) {

        String roomCode =
            (String) session.getAttributes().get("roomCode");

        String name =
            (String) session.getAttributes().get("name");

        if (roomCode == null || name == null) {
            return;
        }

        List<WebSocketSession> roomSessions =
            sessionsByRoom.get(roomCode);

        if (roomSessions != null) {
            roomSessions.remove(session);

            if (roomSessions.isEmpty()) {
                sessionsByRoom.remove(roomCode);
            }
        }

        roomService.removeChatter(roomCode, name);

        System.out.println(name + " disconnected from room " + roomCode);
    }
}
