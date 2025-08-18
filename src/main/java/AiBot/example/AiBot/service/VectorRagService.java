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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;

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
     * Generate embeddings using OpenAI API with retry/backoff and caching
     */
    private List<Double> generateEmbedding(String text) {
        // Return cached embedding if present
        List<Double> cached = embeddingCache.get(text);
        if (cached != null) {
            return cached;
        }

        try {
            String url = "https://api.openai.com/v1/embeddings";
            

            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openaiApiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "text-embedding-ada-002");
            requestBody.put("input", text);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Map> response = postWithRetry(url, request, 5);
            

            
            if (response != null && response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<Map<String, Object>> data = (List<Map<String, Object>>) response.getBody().get("data");
                if (data != null && !data.isEmpty()) {
                    List<Double> embedding = (List<Double>) data.get(0).get("embedding");
                    if (embedding != null) {
                        embeddingCache.put(text, embedding);
                        return embedding;
                    }
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
            String sql = "INSERT INTO document_chunks (document_id, chunk_text, chunk_embedding) VALUES (?, ?, ?::vector)";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (int i = 0; i < chunks.size(); i++) {
                    // Convert Double list to Float array for pgvector compatibility
                    Float[] embeddingArray = embeddings.get(i).stream()
                        .map(Double::floatValue)
                        .toArray(Float[]::new);
                    
                    stmt.setString(1, documentId);
                    stmt.setString(2, chunks.get(i));
                    stmt.setArray(3, conn.createArrayOf("float4", embeddingArray));
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
            // Convert Double list to float array for pgvector compatibility
            Float[] embeddingArray = queryEmbedding.stream()
                .map(Double::floatValue)
                .toArray(Float[]::new);
            
            String sql = """
                SELECT chunk_text, chunk_embedding <=> ?::vector as distance
                FROM document_chunks 
                WHERE document_id = ?
                ORDER BY chunk_embedding <=> ?::vector
                LIMIT ?
                """;
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setArray(1, conn.createArrayOf("float4", embeddingArray));
                stmt.setString(2, documentId);
                stmt.setArray(3, conn.createArrayOf("float4", embeddingArray));
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
     * Generate answer using OpenAI with retrieved context (with retry/backoff)
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
            
            ResponseEntity<Map> response = postWithRetry(url, request, 5);
            
            if (response != null && response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
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
     * HTTP POST with retry/backoff for handling 429/5xx
     */
    private ResponseEntity<Map> postWithRetry(String url, HttpEntity<Map<String, Object>> request, int maxAttempts) {
        long baseDelayMs = 500L;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return restTemplate.postForEntity(url, request, Map.class);
            } catch (HttpClientErrorException e) {
                int status = e.getStatusCode().value();
                if (status == 429) {
                    // Respect Retry-After header if present
                    long delay = parseRetryAfterMillis(e.getResponseHeaders());
                    if (delay <= 0) {
                        delay = (long) Math.min(10000, baseDelayMs * Math.pow(2, attempt - 1));
                    }
                    sleep(delay);
                    continue;
                }
                // Other 4xx: do not retry
                throw e;
            } catch (HttpServerErrorException e) {
                // Retry on 5xx
                long delay = (long) Math.min(10000, baseDelayMs * Math.pow(2, attempt - 1));
                sleep(delay);
            } catch (RestClientException e) {
                // Network/transient error: retry
                long delay = (long) Math.min(10000, baseDelayMs * Math.pow(2, attempt - 1));
                sleep(delay);
            }
        }
        return null;
    }

    private long parseRetryAfterMillis(HttpHeaders headers) {
        if (headers == null) return 0L;
        String retryAfter = headers.getFirst("Retry-After");
        if (retryAfter == null) return 0L;
        try {
            // Retry-After seconds
            long seconds = Long.parseLong(retryAfter.trim());
            return seconds * 1000L;
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
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
