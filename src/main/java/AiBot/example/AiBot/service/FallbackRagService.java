package AiBot.example.AiBot.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class FallbackRagService {

    // In-memory storage for document chunks
    private final Map<String, List<String>> documentChunks = new ConcurrentHashMap<>();
    private final Map<String, String> documentContents = new ConcurrentHashMap<>();

    public void processDocument(String content, String documentId) {
        // Store full content
        documentContents.put(documentId, content);
        
        // Improved chunking with smaller chunks
        List<String> chunkList = new ArrayList<>();
        
        // Split by paragraphs first
        String[] paragraphs = content.split("\n\n");
        
        for (String paragraph : paragraphs) {
            paragraph = paragraph.trim();
            if (!paragraph.isEmpty()) {
                // If paragraph is too long, split it further
                if (paragraph.length() > 500) { // Reduced from 1000 to 500
                    chunkList.addAll(splitLongParagraph(paragraph));
                } else {
                    chunkList.add(paragraph);
                }
            }
        }
        
        documentChunks.put(documentId, chunkList);
        System.out.println("Fallback RAG: Processed document " + documentId + " into " + chunkList.size() + " chunks");
    }

    private List<String> splitLongParagraph(String paragraph) {
        List<String> chunks = new ArrayList<>();
        int maxLength = 500; // Reduced chunk size
        
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

    public String askQuestion(String question, String documentId) {
        String content = documentContents.get(documentId);
        if (content == null) {
            return "Document not found.";
        }

        // Simple keyword-based search
        String lowerQuestion = question.toLowerCase();
        
        // Find relevant chunks based on keyword matching
        List<String> relevantChunks = new ArrayList<>();
        List<String> chunks = documentChunks.get(documentId);
        
        if (chunks != null) {
            for (String chunk : chunks) {
                String lowerChunk = chunk.toLowerCase();
                // Check if chunk contains any words from the question
                String[] questionWords = lowerQuestion.split("\\s+");
                for (String word : questionWords) {
                    if (word.length() > 3 && lowerChunk.contains(word)) {
                        relevantChunks.add(chunk);
                        break;
                    }
                }
            }
        }

        // If no relevant chunks found, use the first few paragraphs
        if (relevantChunks.isEmpty() && chunks != null && !chunks.isEmpty()) {
            relevantChunks = chunks.subList(0, Math.min(3, chunks.size()));
        }

        // Generate response based on relevant chunks
        StringBuilder response = new StringBuilder();
        
        if (!relevantChunks.isEmpty()) {
            response.append("Based on the document content, here's what I found:\n\n");
            
            // Show more relevant content
            for (String chunk : relevantChunks) {
                response.append("• ").append(chunk.substring(0, Math.min(300, chunk.length())));
                if (chunk.length() > 300) response.append("...");
                response.append("\n\n");
            }
            
            // Try to provide a direct answer if possible
            if (lowerQuestion.contains("annual") || lowerQuestion.contains("leave") || lowerQuestion.contains("vacation")) {
                response.append("💡 **Summary**: Based on the document, ");
                if (lowerQuestion.contains("how many")) {
                    response.append("the specific number of annual leaves should be mentioned in the relevant sections above.");
                } else {
                    response.append("annual leave policies are covered in the document.");
                }
            }
        } else {
            response.append("I couldn't find specific information about your question in the document. ");
            response.append("Please try rephrasing your question or check if the information is in a different section.");
        }
        
        // Add a note about OpenAI requirement
        response.append("\n\n---\n*Note: This is a fallback response. For better AI-powered answers with OpenAI, ");
        response.append("please set your API key in application.properties.*");
        
        return response.toString();
    }

    public void clearDocuments() {
        documentChunks.clear();
        documentContents.clear();
    }
}
