package AiBot.example.AiBot.service;

import AiBot.example.AiBot.model.Document;
import AiBot.example.AiBot.model.QueryLog;
import AiBot.example.AiBot.repository.DocumentRepository;
import AiBot.example.AiBot.repository.QueryLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class QueryService {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private QueryLogRepository queryLogRepository;

    @Autowired
    private DocumentService documentService;

    public Map<String, Object> askQuestion(String question, String username, Long documentId) {
        try {
            // Use RAG-based question answering
            String answer = documentService.askQuestionAboutDocument(question, documentId);
            
            // Save query log
            QueryLog queryLog = new QueryLog(username, question, answer, documentId);
            queryLogRepository.save(queryLog);
            
            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("question", question);
            response.put("answer", answer);
            response.put("documentId", documentId);
            response.put("timestamp", queryLog.getTimestamp());
            
            return response;
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to process question", e);
        }
    }

    public List<QueryLog> getQueryHistory(String username) {
        return queryLogRepository.findByUsernameOrderByTimestampDesc(username);
    }

    public List<QueryLog> getAllQueryHistory() {
        return queryLogRepository.findAll();
    }

    public Map<String, Object> getDocumentSummary(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found"));
        
        Map<String, Object> response = new HashMap<>();
        response.put("documentId", documentId);
        response.put("fileName", document.getFileName());
        response.put("summary", document.getSummary());
        response.put("uploadedBy", document.getUploadedBy());
        response.put("uploadedAt", document.getUploadedAt());
        
        return response;
    }
}
