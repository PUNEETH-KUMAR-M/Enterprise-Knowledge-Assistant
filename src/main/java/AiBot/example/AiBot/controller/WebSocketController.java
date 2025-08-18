package AiBot.example.AiBot.controller;

import AiBot.example.AiBot.model.ChatMessage;
import AiBot.example.AiBot.service.QueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Controller
public class WebSocketController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private QueryService queryService;

    @MessageMapping("/chat.sendMessage")
    @SendTo("/topic/public")
    public ChatMessage sendMessage(@Payload ChatMessage chatMessage) {
        return chatMessage;
    }

    @MessageMapping("/chat.addUser")
    @SendTo("/topic/public")
    public ChatMessage addUser(@Payload ChatMessage chatMessage, SimpMessageHeaderAccessor headerAccessor) {
        // Add username in web socket session
        headerAccessor.getSessionAttributes().put("username", chatMessage.getSender());
        return chatMessage;
    }

    @MessageMapping("/chat.askQuestion")
    public void askQuestion(@Payload Map<String, Object> request, SimpMessageHeaderAccessor headerAccessor) {
        String username = (String) request.get("username");
        String question = (String) request.get("question");
        String documentId = request.get("documentId").toString();
        String sessionId = headerAccessor.getSessionId();

        // Send typing indicator to the current STOMP session
        ChatMessage typingMessage = new ChatMessage("TYPING", "🤔 Thinking...", "AiBot", documentId);
        sendToSessionQueue(sessionId, typingMessage);

        // Process question asynchronously
        CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> response = queryService.askQuestion(question, username, Long.valueOf(documentId));
                String answer = (String) response.get("answer");

                ChatMessage answerMessage = new ChatMessage("ANSWER", answer, "AiBot", documentId);
                sendToSessionQueue(sessionId, answerMessage);
            } catch (Exception e) {
                ChatMessage errorMessage = new ChatMessage("ERROR", "Sorry, I encountered an error: " + e.getMessage(), "AiBot", documentId);
                sendToSessionQueue(sessionId, errorMessage);
            }
        });
    }

    @MessageMapping("/chat.typing")
    public void typing(@Payload Map<String, Object> request, SimpMessageHeaderAccessor headerAccessor) {
        String username = (String) request.get("username");
        String documentId = request.get("documentId").toString();
        String sessionId = headerAccessor.getSessionId();

        ChatMessage typingMessage = new ChatMessage("TYPING", username + " is typing...", username, documentId);
        sendToSessionQueue(sessionId, typingMessage);
    }

    private void sendToSessionQueue(String sessionId, ChatMessage message) {
        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create();
        headers.setSessionId(sessionId);
        headers.setLeaveMutable(true);
        messagingTemplate.convertAndSendToUser(sessionId, "/queue/chat", message, headers.getMessageHeaders());
    }
}
