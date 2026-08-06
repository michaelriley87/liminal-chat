package com.michaelriley87.liminal_chat;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;
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

  private static final int MAX_NAME_LENGTH = 24;
  private static final int MAX_MESSAGE_LENGTH = 500;

  private static final Pattern ROOM_CODE_PATTERN = Pattern.compile("[A-Z0-9]{5}");

  private static final CloseStatus ROOM_UNAVAILABLE = new CloseStatus(4001, "Room unavailable");

  private static final CloseStatus NAME_TAKEN =
      new CloseStatus(4002, "Display name already in use");

  private static final CloseStatus ROOM_EXPIRED = new CloseStatus(4001, "Room expired");

  private static final CloseStatus INVALID_CONNECTION =
      new CloseStatus(4003, "Invalid connection details");

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
      session.close(INVALID_CONNECTION);
      return;
    }

    var parameters = UriComponentsBuilder.fromUri(session.getUri()).build().getQueryParams();

    String roomCodeParameter = parameters.getFirst("room");
    String nameParameter = parameters.getFirst("name");

    if (roomCodeParameter == null || nameParameter == null) {
      session.close(INVALID_CONNECTION);
      return;
    }

    String roomCode;
    String name;

    try {
      roomCode =
          URLDecoder.decode(roomCodeParameter, StandardCharsets.UTF_8)
              .trim()
              .toUpperCase(Locale.ROOT);
      name = URLDecoder.decode(nameParameter, StandardCharsets.UTF_8).trim();
    } catch (IllegalArgumentException exception) {
      session.close(INVALID_CONNECTION);
      return;
    }

    if (!ROOM_CODE_PATTERN.matcher(roomCode).matches()
        || name.isBlank()
        || name.length() > MAX_NAME_LENGTH) {
      session.close(INVALID_CONNECTION);
      return;
    }

    RoomService.AddChatterResult addResult = roomService.tryAddChatter(roomCode, new Chatter(name));

    if (addResult == RoomService.AddChatterResult.ROOM_UNAVAILABLE) {
      session.close(ROOM_UNAVAILABLE);
      return;
    }

    if (addResult == RoomService.AddChatterResult.NAME_TAKEN) {
      session.close(NAME_TAKEN);
      return;
    }

    session.getAttributes().put("roomCode", roomCode);
    session.getAttributes().put("name", name);

    sessionsByRoom.computeIfAbsent(roomCode, code -> new CopyOnWriteArrayList<>()).add(session);

    broadcastSystemMessage(roomCode, name + " joined the room");
    broadcastParticipantList(roomCode);

    System.out.println(name + " connected to room " + roomCode);
  }

  @Override
  protected void handleTextMessage(WebSocketSession senderSession, TextMessage message)
      throws Exception {
    String roomCode = (String) senderSession.getAttributes().get("roomCode");
    String name = (String) senderSession.getAttributes().get("name");

    if (roomCode == null || name == null || roomService.getRoom(roomCode) == null) {
      return;
    }

    ChatMessage incomingMessage;

    try {
      incomingMessage = objectMapper.readValue(message.getPayload(), ChatMessage.class);
    } catch (Exception exception) {
      return;
    }

    if (!CHAT_MESSAGE.equals(incomingMessage.getType())) {
      return;
    }

    String content = incomingMessage.getContent();

    if (content == null) {
      return;
    }

    String trimmedContent = content.trim();

    if (trimmedContent.isBlank() || trimmedContent.length() > MAX_MESSAGE_LENGTH) {
      return;
    }

    roomService.updateRoomActivity(roomCode);

    ChatMessage outgoingMessage = new ChatMessage(CHAT_MESSAGE, name, trimmedContent);

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
          session.close(ROOM_EXPIRED);
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
