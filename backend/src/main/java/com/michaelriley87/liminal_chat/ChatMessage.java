package com.michaelriley87.liminal_chat;

import java.util.List;

public class ChatMessage {

  private String type;
  private String sender;
  private String content;
  private List<String> participants;

  public ChatMessage() {}

  public ChatMessage(String type, String sender, String content) {
    this.type = type;
    this.sender = sender;
    this.content = content;
  }

  public ChatMessage(String type, List<String> participants) {
    this.type = type;
    this.participants = participants;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getSender() {
    return sender;
  }

  public void setSender(String sender) {
    this.sender = sender;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public List<String> getParticipants() {
    return participants;
  }

  public void setParticipants(List<String> participants) {
    this.participants = participants;
  }
}
