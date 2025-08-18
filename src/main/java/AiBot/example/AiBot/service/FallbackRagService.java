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
            response.append("📄 **Based on the document content:**\n\n");
            
            // Show more relevant content
            for (int i = 0; i < relevantChunks.size(); i++) {
                String chunk = relevantChunks.get(i);
                response.append("**Section ").append(i + 1).append(":**\n");
                response.append(chunk.substring(0, Math.min(400, chunk.length())));
                if (chunk.length() > 400) response.append("...");
                response.append("\n\n");
            }
            
            // Try to provide a direct answer based on question patterns
            response.append("💡 **Analysis**: ");
            if (lowerQuestion.contains("how many") || lowerQuestion.contains("number")) {
                response.append("Look for specific numbers or quantities in the sections above.");
            } else if (lowerQuestion.contains("when") || lowerQuestion.contains("date")) {
                response.append("Check for dates, deadlines, or time-related information in the relevant sections.");
            } else if (lowerQuestion.contains("what") || lowerQuestion.contains("define")) {
                response.append("The definitions and explanations should be found in the relevant sections above.");
            } else if (lowerQuestion.contains("who") || lowerQuestion.contains("responsible")) {
                response.append("Look for names, roles, or responsibilities mentioned in the sections above.");
            } else if (lowerQuestion.contains("where")) {
                response.append("Check for location or department information in the relevant sections.");
            } else if (lowerQuestion.contains("why") || lowerQuestion.contains("reason")) {
                response.append("The rationale or reasons should be explained in the relevant sections above.");
            } else if (lowerQuestion.contains("how")) {
                response.append("Look for step-by-step processes or procedures in the sections above.");
            } else {
                response.append("The answer to your question should be found in the relevant sections above.");
            }
        } else {
            response.append("❓ **No specific matches found.**\n\n");
            response.append("I couldn't find content directly related to your question. This could mean:\n");
            response.append("• The information might be in a different document\n");
            response.append("• Try using different keywords\n");
            response.append("• The document might not contain this specific information\n\n");
            response.append("**Suggestion**: Try rephrasing your question with different terms or check if the information exists in the document.");
        }
        
        // Add a note about service type
        response.append("\n\n---\n*Note: Using text-based search service. For AI-powered semantic search, OpenAI API access is required.*");
        
        return response.toString();
    }

    public void clearDocuments() {
        documentChunks.clear();
        documentContents.clear();
    }
}
