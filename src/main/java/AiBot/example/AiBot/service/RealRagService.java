package AiBot.example.AiBot.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RealRagService {

    @Value("${spring.ai.openai.api-key}")
    private String openaiApiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    
    // In-memory storage for document chunks (in production, use vector database)
    private final Map<String, List<String>> documentChunks = new ConcurrentHashMap<>();
    private final Map<String, String> documentContents = new ConcurrentHashMap<>();

    public void processDocument(String content, String documentId) {
        // Store full content
        documentContents.put(documentId, content);
        
        // Split document into chunks
        List<String> chunks = chunkDocument(content);
        documentChunks.put(documentId, chunks);
        
        System.out.println("Real RAG: Processed document " + documentId + " into " + chunks.size() + " chunks");
        
        // In production, you would:
        // 1. Generate embeddings for each chunk using OpenAI
        // 2. Store embeddings in vector database (PostgreSQL with pgvector)
    }

    public String askQuestion(String question, String documentId) {
        String content = documentContents.get(documentId);
        if (content == null) {
            return "Document not found.";
        }

        try {
            // Find relevant chunks (in production, use semantic search with embeddings)
            List<String> relevantChunks = findRelevantChunks(question, documentId);
            
            // Build context from relevant chunks
            StringBuilder context = new StringBuilder();
            for (String chunk : relevantChunks) {
                context.append(chunk).append("\n\n");
            }
            
            // Generate answer using OpenAI
            return generateAnswerWithOpenAI(question, context.toString());
            
        } catch (Exception e) {
            return "Error processing question: " + e.getMessage();
        }
    }

    private List<String> findRelevantChunks(String question, String documentId) {
        List<String> chunks = documentChunks.get(documentId);
        if (chunks == null || chunks.isEmpty()) {
            return new ArrayList<>();
        }

        // Simple keyword-based search (in production, use semantic search)
        List<String> relevantChunks = new ArrayList<>();
        String lowerQuestion = question.toLowerCase();
        
        for (String chunk : chunks) {
            String lowerChunk = chunk.toLowerCase();
            String[] questionWords = lowerQuestion.split("\\s+");
            
            for (String word : questionWords) {
                if (word.length() > 3 && lowerChunk.contains(word)) {
                    relevantChunks.add(chunk);
                    break;
                }
            }
        }

        // If no relevant chunks found, use first few
        if (relevantChunks.isEmpty() && !chunks.isEmpty()) {
            relevantChunks = chunks.subList(0, Math.min(3, chunks.size()));
        }

        return relevantChunks;
    }

    private String generateAnswerWithOpenAI(String question, String context) {
        try {
            String url = "https://api.openai.com/v1/chat/completions";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openaiApiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "gpt-4o-mini");
            requestBody.put("messages", Arrays.asList(
                Map.of("role", "system", "content", 
                    "You are a helpful assistant that answers questions based on the provided document context. " +
                    "Only use information from the context to answer questions. If the context doesn't contain " +
                    "enough information to answer the question, say so."),
                Map.of("role", "user", "content", 
                    "Context:\n" + context + "\n\nQuestion: " + question)
            ));
            requestBody.put("max_tokens", 500);
            requestBody.put("temperature", 0.7);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    return (String) message.get("content");
                }
            }
            
            return "Failed to generate answer from OpenAI.";
            
        } catch (Exception e) {
            return "Error calling OpenAI API: " + e.getMessage();
        }
    }

    private List<String> chunkDocument(String content) {
        // Split by paragraphs
        String[] paragraphs = content.split("\n\n");
        List<String> chunks = new ArrayList<>();
        
        for (String paragraph : paragraphs) {
            paragraph = paragraph.trim();
            if (!paragraph.isEmpty()) {
                // If paragraph is too long, split it further
                if (paragraph.length() > 1000) {
                    chunks.addAll(splitLongParagraph(paragraph));
                } else {
                    chunks.add(paragraph);
                }
            }
        }
        
        return chunks;
    }

    private List<String> splitLongParagraph(String paragraph) {
        List<String> chunks = new ArrayList<>();
        int maxLength = 1000;
        
        if (paragraph.length() <= maxLength) {
            chunks.add(paragraph);
            return chunks;
        }
        
        // Split by sentences
        String[] sentences = paragraph.split("(?<=[.!?])\\s+");
        StringBuilder currentChunk = new StringBuilder();
        
        for (String sentence : sentences) {
            if (currentChunk.length() + sentence.length() > maxLength) {
                if (currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString().trim());
                    currentChunk = new StringBuilder();
                }
            }
            currentChunk.append(sentence).append(" ");
        }
        
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }
        
        return chunks;
    }
}
