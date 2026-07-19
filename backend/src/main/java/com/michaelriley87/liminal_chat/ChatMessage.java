package com.michaelriley87.liminal_chat;

public class ChatMessage {
    private String type;
    private String sender;
    private String content;

    public ChatMessage() {
    }

    public ChatMessage(String type, String sender, String content) {
        this.type = type;
        this.sender = sender;
        this.content = content;
    }

    public String getType() {
        return type;
    }

    public String getSender() {

        return sender;
    }

    public String getContent() {
        return content;
    }
}
