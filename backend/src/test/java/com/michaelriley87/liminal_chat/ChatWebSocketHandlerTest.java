package com.michaelriley87.liminal_chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.ObjectMapper;

class ChatWebSocketHandlerTest {

  private RoomService roomService;
  private ObjectMapper objectMapper;
  private ChatWebSocketHandler handler;

  @BeforeEach
  void setUp() {
    roomService = new RoomService();
    objectMapper = new ObjectMapper();
    handler = new ChatWebSocketHandler(roomService, objectMapper);
  }

  @Test
  void acceptsValidConnectionAndNormalisesDetails() throws Exception {
    Room room = roomService.createRoom();
    WebSocketSession session =
        session("/ws?room=" + room.getCode().toLowerCase() + "&name=%20Alice%20");

    handler.afterConnectionEstablished(session);

    assertEquals(room.getCode(), session.getAttributes().get("roomCode"));
    assertEquals("Alice", session.getAttributes().get("name"));
    assertEquals(java.util.List.of("Alice"), roomService.getChatterNames(room.getCode()));
    verify(session, times(2)).sendMessage(any(TextMessage.class));
  }

  @Test
  void rejectsMissingConnectionDetails() throws Exception {
    WebSocketSession session = session("/ws?room=ABCDE");

    handler.afterConnectionEstablished(session);

    assertClosedWith(session, 4003, "Invalid connection details");
  }

  @Test
  void rejectsInvalidRoomCodeAndName() throws Exception {
    WebSocketSession invalidCode = session("/ws?room=ABCD&name=Alice");
    WebSocketSession blankName = session("/ws?room=ABCDE&name=%20%20");
    WebSocketSession longName = session("/ws?room=ABCDE&name=" + "a".repeat(25));

    handler.afterConnectionEstablished(invalidCode);
    handler.afterConnectionEstablished(blankName);
    handler.afterConnectionEstablished(longName);

    assertClosedWith(invalidCode, 4003, "Invalid connection details");
    assertClosedWith(blankName, 4003, "Invalid connection details");
    assertClosedWith(longName, 4003, "Invalid connection details");
  }

  @Test
  void rejectsUnavailableRoom() throws Exception {
    WebSocketSession session = session("/ws?room=ABCDE&name=Alice");

    handler.afterConnectionEstablished(session);

    assertClosedWith(session, 4001, "Room unavailable");
  }

  @Test
  void rejectsDuplicateNameIgnoringCapitalisation() throws Exception {
    Room room = roomService.createRoom();
    WebSocketSession firstSession = session("/ws?room=" + room.getCode() + "&name=Alice");
    WebSocketSession secondSession = session("/ws?room=" + room.getCode() + "&name=alice");

    handler.afterConnectionEstablished(firstSession);
    handler.afterConnectionEstablished(secondSession);

    assertClosedWith(secondSession, 4002, "Display name already in use");
  }

  @Test
  void broadcastsTrimmedMessageUsingSessionName() throws Exception {
    WebSocketSession session = connect("Alice");
    clearInvocations(session);

    handler.handleMessage(
        session,
        new TextMessage(
            "{\"type\":\"CHAT_MESSAGE\",\"sender\":\"Mallory\",\"content\":\"  hello  \"}"));

    ArgumentCaptor<TextMessage> messageCaptor = ArgumentCaptor.forClass(TextMessage.class);
    verify(session).sendMessage(messageCaptor.capture());
    ChatMessage outgoing =
        objectMapper.readValue(messageCaptor.getValue().getPayload(), ChatMessage.class);
    assertEquals("CHAT_MESSAGE", outgoing.getType());
    assertEquals("Alice", outgoing.getSender());
    assertEquals("hello", outgoing.getContent());
  }

  @Test
  void ignoresMalformedOrInvalidMessages() throws Exception {
    WebSocketSession session = connect("Alice");
    clearInvocations(session);

    handler.handleMessage(session, new TextMessage("not-json"));
    handler.handleMessage(
        session, new TextMessage("{\"type\":\"SYSTEM_MESSAGE\",\"content\":\"hello\"}"));
    handler.handleMessage(session, new TextMessage("{\"type\":\"CHAT_MESSAGE\"}"));
    handler.handleMessage(
        session, new TextMessage("{\"type\":\"CHAT_MESSAGE\",\"content\":\"   \"}"));
    handler.handleMessage(
        session,
        new TextMessage("{\"type\":\"CHAT_MESSAGE\",\"content\":\"" + "a".repeat(501) + "\"}"));

    verify(session, never()).sendMessage(any(TextMessage.class));
  }

  @Test
  void acceptsMessageAtMaximumLength() throws Exception {
    WebSocketSession session = connect("Alice");
    clearInvocations(session);

    handler.handleMessage(
        session,
        new TextMessage("{\"type\":\"CHAT_MESSAGE\",\"content\":\"" + "a".repeat(500) + "\"}"));

    verify(session).sendMessage(any(TextMessage.class));
  }

  @Test
  void removesParticipantAndBroadcastsWhenConnectionCloses() throws Exception {
    WebSocketSession session = connect("Alice");
    String roomCode = (String) session.getAttributes().get("roomCode");
    clearInvocations(session);

    handler.afterConnectionClosed(session, CloseStatus.NORMAL);

    assertTrue(roomService.getChatterNames(roomCode).isEmpty());
    verify(session, never()).sendMessage(any(TextMessage.class));
  }

  @Test
  void closesSessionsWhenRoomExpires() throws Exception {
    WebSocketSession session = connect("Alice");
    String roomCode = (String) session.getAttributes().get("roomCode");
    clearInvocations(session);

    handler.closeRoomSessions(roomCode);

    assertClosedWith(session, 4001, "Room expired");
  }

  private WebSocketSession connect(String name) throws Exception {
    Room room = roomService.createRoom();
    WebSocketSession session = session("/ws?room=" + room.getCode() + "&name=" + name);
    handler.afterConnectionEstablished(session);
    return session;
  }

  private WebSocketSession session(String pathAndQuery) {
    WebSocketSession session = mock(WebSocketSession.class);
    Map<String, Object> attributes = new HashMap<>();
    when(session.getUri()).thenReturn(URI.create("http://localhost" + pathAndQuery));
    when(session.getAttributes()).thenReturn(attributes);
    when(session.isOpen()).thenReturn(true);
    return session;
  }

  private void assertClosedWith(WebSocketSession session, int code, String reason)
      throws Exception {
    ArgumentCaptor<CloseStatus> statusCaptor = ArgumentCaptor.forClass(CloseStatus.class);
    verify(session).close(statusCaptor.capture());
    assertEquals(code, statusCaptor.getValue().getCode());
    assertEquals(reason, statusCaptor.getValue().getReason());
  }
}
