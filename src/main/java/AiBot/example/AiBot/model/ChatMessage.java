package AiBot.example.AiBot.model;

import java.time.LocalDateTime;

public class ChatMessage {
    private String type; // "QUESTION", "ANSWER", "TYPING", "ERROR"
    private String content;
    private String sender;
    private String documentId;
    private LocalDateTime timestamp;
    private String messageId;

    public ChatMessage() {
        this.timestamp = LocalDateTime.now();
    }

    public ChatMessage(String type, String content, String sender, String documentId) {
        this.type = type;
        this.content = content;
        this.sender = sender;
        this.documentId = documentId;
        this.timestamp = LocalDateTime.now();
        this.messageId = String.valueOf(System.currentTimeMillis());
    }

    // Getters and Setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
}
