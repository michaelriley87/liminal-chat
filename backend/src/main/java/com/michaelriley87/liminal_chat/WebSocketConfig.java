package com.michaelriley87.liminal_chat;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
  private final ChatWebSocketHandler chatWebSocketHandler;
  private final String[] allowedOrigins;

  public WebSocketConfig(
      ChatWebSocketHandler chatWebSocketHandler,
      @Value("${liminal.allowed-origins}") String[] allowedOrigins) {
    this.chatWebSocketHandler = chatWebSocketHandler;
    this.allowedOrigins = allowedOrigins;
  }

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {

    registry.addHandler(chatWebSocketHandler, "/ws").setAllowedOrigins(allowedOrigins);
  }
}
