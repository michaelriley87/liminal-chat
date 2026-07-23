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

  private static final String CHAT_MESSAGE = "CHAT_MESSAGE";
  private static final String SYSTEM_MESSAGE = "SYSTEM_MESSAGE";
  private static final String PARTICIPANT_LIST = "PARTICIPANT_LIST";

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

    var parameters = UriComponentsBuilder.fromUri(session.getUri()).build().getQueryParams();

    String roomCode = parameters.getFirst("room");
    String name = parameters.getFirst("name");

    if (roomCode == null
        || name == null
        || name.isBlank()
        || roomService.getRoom(roomCode) == null) {
      session.close(CloseStatus.BAD_DATA);
      return;
    }

    String trimmedName = name.trim();

    session.getAttributes().put("roomCode", roomCode);
    session.getAttributes().put("name", trimmedName);

    sessionsByRoom.computeIfAbsent(roomCode, code -> new CopyOnWriteArrayList<>()).add(session);
    roomService.addChatter(roomCode, new Chatter(trimmedName));

    broadcastSystemMessage(roomCode, trimmedName + " joined the room");
    broadcastParticipantList(roomCode);

    System.out.println(trimmedName + " connected to room " + roomCode);
  }

  @Override
  protected void handleTextMessage(WebSocketSession senderSession, TextMessage message)
      throws Exception {
    String roomCode = (String) senderSession.getAttributes().get("roomCode");
    String name = (String) senderSession.getAttributes().get("name");

    if (roomCode == null || name == null) {
      return;
    }

    ChatMessage incomingMessage = objectMapper.readValue(message.getPayload(), ChatMessage.class);
    String content = incomingMessage.getContent();

    if (content == null || content.isBlank()) {
      return;
    }

    roomService.updateRoomActivity(roomCode);

    ChatMessage outgoingMessage = new ChatMessage(CHAT_MESSAGE, name, content.trim());

    broadcast(roomCode, outgoingMessage);
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
    String roomCode = (String) session.getAttributes().get("roomCode");
    String name = (String) session.getAttributes().get("name");

    if (roomCode == null || name == null) {
      return;
    }

    List<WebSocketSession> roomSessions = sessionsByRoom.get(roomCode);

    if (roomSessions != null) {
      roomSessions.remove(session);

      if (roomSessions.isEmpty()) {
        sessionsByRoom.remove(roomCode);
      }
    }

    roomService.removeChatter(roomCode, name);

    if (roomService.getRoom(roomCode) != null) {
      broadcastSystemMessage(roomCode, name + " left the room");
      broadcastParticipantList(roomCode);
    }

    System.out.println(name + " disconnected from room " + roomCode);
  }

  public void closeRoomSessions(String roomCode) {
    List<WebSocketSession> roomSessions = sessionsByRoom.remove(roomCode);

    if (roomSessions == null) {
      return;
    }

    for (WebSocketSession session : roomSessions) {
      if (session.isOpen()) {
        try {
          session.close(CloseStatus.NORMAL.withReason("Room expired"));
        } catch (Exception exception) {
          System.out.println("Failed to close WebSocket session: " + exception.getMessage());
        }
      }
    }
  }

  private void broadcastSystemMessage(String roomCode, String content) throws Exception {
    broadcast(roomCode, new ChatMessage(SYSTEM_MESSAGE, "", content));
  }

  private void broadcastParticipantList(String roomCode) throws Exception {
    ChatMessage participantMessage =
        new ChatMessage(PARTICIPANT_LIST, roomService.getChatterNames(roomCode));

    broadcast(roomCode, participantMessage);
  }

  private void broadcast(String roomCode, ChatMessage message) throws Exception {
    List<WebSocketSession> roomSessions = sessionsByRoom.get(roomCode);

    if (roomSessions == null) {
      return;
    }

    String outgoingJson = objectMapper.writeValueAsString(message);
    TextMessage outgoingMessage = new TextMessage(outgoingJson);

    for (WebSocketSession session : roomSessions) {
      if (session.isOpen()) {
        synchronized (session) {
          session.sendMessage(outgoingMessage);
        }
      }
    }
  }
}
