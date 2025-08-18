package AiBot.example.AiBot.controller;

import AiBot.example.AiBot.model.QueryLog;
import AiBot.example.AiBot.service.QueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/query")
@CrossOrigin(origins = "*")
public class QueryController {

    @Autowired
    private QueryService queryService;

    @PostMapping("/ask")
    public ResponseEntity<Map<String, Object>> askQuestion(@RequestBody Map<String, Object> request) {
        try {
            Long documentId = Long.valueOf(request.get("documentId").toString());
            String question = (String) request.get("question");
            String username = (String) request.get("username");
            
            if (documentId == null || question == null || question.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Document ID and question are required"));
            }
            
            Map<String, Object> response = queryService.askQuestion(question, username, documentId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/history/{username}")
    public ResponseEntity<List<QueryLog>> getQueryHistory(@PathVariable String username) {
        try {
            List<QueryLog> history = queryService.getQueryHistory(username);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/history")
    public ResponseEntity<List<QueryLog>> getAllQueryHistory() {
        try {
            List<QueryLog> history = queryService.getAllQueryHistory();
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/document/{documentId}/summary")
    public ResponseEntity<Map<String, Object>> getDocumentSummary(@PathVariable Long documentId) {
        try {
            Map<String, Object> summary = queryService.getDocumentSummary(documentId);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
