package AiBot.example.AiBot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class VectorRagService {

    @Value("${spring.ai.openai.api-key}")
    private String openaiApiKey;

    @Autowired
    private DataSource dataSource;

    private final RestTemplate restTemplate = new RestTemplate();
    
    // Cache for embeddings to avoid regenerating them
    private final Map<String, List<Double>> embeddingCache = new ConcurrentHashMap<>();

    /**
     * Process document: chunk, embed, and store in vector database
     */
    public void processDocument(String content, String documentId) {
        try {
            // 1. Split document into chunks
            List<String> chunks = chunkDocument(content);
            System.out.println("Document " + documentId + " split into " + chunks.size() + " chunks");
            
            // 2. Generate embeddings for each chunk
            List<List<Double>> embeddings = new ArrayList<>();
            for (String chunk : chunks) {
                List<Double> embedding = generateEmbedding(chunk);
                embeddings.add(embedding);
            }
            
            // 3. Store chunks and embeddings in vector database
            storeInVectorDatabase(documentId, chunks, embeddings);
            
            System.out.println("Document " + documentId + " processed and stored in vector database");
            
        } catch (Exception e) {
            System.err.println("Error processing document " + documentId + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Ask question using RAG: retrieve relevant chunks and generate answer
     */
    public String askQuestion(String question, String documentId) {
        try {
            // 1. Generate embedding for the question
            List<Double> questionEmbedding = generateEmbedding(question);
            
            // 2. Find most similar chunks using vector similarity search
            List<String> relevantChunks = findSimilarChunks(questionEmbedding, documentId, 3);
            
            if (relevantChunks.isEmpty()) {
                return "I couldn't find relevant information in the document to answer your question.";
            }
            
            // 3. Build context from relevant chunks
            StringBuilder context = new StringBuilder();
            for (String chunk : relevantChunks) {
                context.append(chunk).append("\n\n");
            }
            
            // 4. Generate answer using OpenAI with retrieved context
            return generateAnswerWithOpenAI(question, context.toString());
            
        } catch (Exception e) {
            return "Error processing question: " + e.getMessage();
        }
    }

    /**
     * Generate embeddings using OpenAI API
     */
    private List<Double> generateEmbedding(String text) {
        try {
            String url = "https://api.openai.com/v1/embeddings";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openaiApiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "text-embedding-3-small");
            requestBody.put("input", text);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<Map<String, Object>> data = (List<Map<String, Object>>) response.getBody().get("data");
                if (data != null && !data.isEmpty()) {
                    List<Double> embedding = (List<Double>) data.get(0).get("embedding");
                    return embedding;
                }
            }
            
            throw new RuntimeException("Failed to generate embedding");
            
        } catch (Exception e) {
            throw new RuntimeException("Error calling OpenAI embedding API: " + e.getMessage());
        }
    }

    /**
     * Store chunks and embeddings in PostgreSQL with pgvector
     */
    private void storeInVectorDatabase(String documentId, List<String> chunks, List<List<Double>> embeddings) {
        try (Connection conn = dataSource.getConnection()) {
            // Create table if not exists
            createVectorTableIfNotExists(conn);
            
            // Insert chunks and embeddings
            String sql = "INSERT INTO document_chunks (document_id, chunk_text, chunk_embedding) VALUES (?, ?, ?)";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (int i = 0; i < chunks.size(); i++) {
                    stmt.setString(1, documentId);
                    stmt.setString(2, chunks.get(i));
                    stmt.setArray(3, conn.createArrayOf("float8", embeddings.get(i).toArray()));
                    stmt.executeUpdate();
                }
            }
            
        } catch (SQLException e) {
            throw new RuntimeException("Error storing in vector database: " + e.getMessage());
        }
    }

    /**
     * Create vector table with pgvector extension
     */
    private void createVectorTableIfNotExists(Connection conn) throws SQLException {
        // Enable pgvector extension
        try (PreparedStatement stmt = conn.prepareStatement("CREATE EXTENSION IF NOT EXISTS vector")) {
            stmt.execute();
        }
        
        // Create table for document chunks
        String createTableSql = """
            CREATE TABLE IF NOT EXISTS document_chunks (
                id SERIAL PRIMARY KEY,
                document_id VARCHAR(255) NOT NULL,
                chunk_text TEXT NOT NULL,
                chunk_embedding vector(1536),
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;
        
        try (PreparedStatement stmt = conn.prepareStatement(createTableSql)) {
            stmt.execute();
        }
        
        // Create index for similarity search
        String createIndexSql = "CREATE INDEX IF NOT EXISTS idx_chunk_embedding ON document_chunks USING ivfflat (chunk_embedding vector_cosine_ops)";
        try (PreparedStatement stmt = conn.prepareStatement(createIndexSql)) {
            stmt.execute();
        }
    }

    /**
     * Find similar chunks using vector similarity search
     */
    private List<String> findSimilarChunks(List<Double> queryEmbedding, String documentId, int limit) {
        try (Connection conn = dataSource.getConnection()) {
            String sql = """
                SELECT chunk_text, chunk_embedding <=> ? as distance
                FROM document_chunks 
                WHERE document_id = ?
                ORDER BY chunk_embedding <=> ?
                LIMIT ?
                """;
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setArray(1, conn.createArrayOf("float8", queryEmbedding.toArray()));
                stmt.setString(2, documentId);
                stmt.setArray(3, conn.createArrayOf("float8", queryEmbedding.toArray()));
                stmt.setInt(4, limit);
                
                List<String> chunks = new ArrayList<>();
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        chunks.add(rs.getString("chunk_text"));
                    }
                }
                return chunks;
            }
            
        } catch (SQLException e) {
            throw new RuntimeException("Error finding similar chunks: " + e.getMessage());
        }
    }

    /**
     * Generate answer using OpenAI with retrieved context
     */
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
                    "enough information to answer the question, say so. Be concise and accurate."),
                Map.of("role", "user", "content", 
                    "Context:\n" + context + "\n\nQuestion: " + question)
            ));
            requestBody.put("max_tokens", 500);
            requestBody.put("temperature", 0.3);

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

    /**
     * Split document into chunks for processing
     */
    private List<String> chunkDocument(String content) {
        // Split by paragraphs first
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

    /**
     * Split long paragraphs into smaller chunks
     */
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

    /**
     * Clear all data for a document
     */
    public void clearDocument(String documentId) {
        try (Connection conn = dataSource.getConnection()) {
            String sql = "DELETE FROM document_chunks WHERE document_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, documentId);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error clearing document: " + e.getMessage());
        }
    }
}
