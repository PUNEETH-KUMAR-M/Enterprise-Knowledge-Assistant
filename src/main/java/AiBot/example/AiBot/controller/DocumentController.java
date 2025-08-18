package AiBot.example.AiBot.controller;

import AiBot.example.AiBot.model.Document;
import AiBot.example.AiBot.service.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/documents")
@CrossOrigin(origins = "*")
public class DocumentController {

    @Autowired
    private DocumentService documentService;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadDocument(@RequestParam("file") MultipartFile file,
                                                   @RequestParam("uploadedBy") String uploadedBy) {
        try {
            // Validate input parameters
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "File is required and cannot be empty"));
            }
            
            if (uploadedBy == null || uploadedBy.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "UploadedBy parameter is required"));
            }
            
            System.out.println("Received upload request for file: " + file.getOriginalFilename() + " by user: " + uploadedBy);
            
            Document savedDoc = documentService.uploadDocument(file, uploadedBy);
            return ResponseEntity.ok(Map.of(
                "message", "Document uploaded successfully",
                "document", Map.of(
                    "id", savedDoc.getId(),
                    "fileName", savedDoc.getFileName(),
                    "uploadedBy", savedDoc.getUploadedBy(),
                    "uploadedAt", savedDoc.getUploadedAt(),
                    "summary", savedDoc.getSummary()
                )
            ));
        } catch (IOException e) {
            System.err.println("File upload failed: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", "File upload failed: " + e.getMessage()));
        } catch (Exception e) {
            System.err.println("Upload error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<Document>> getAllDocuments() {
        return ResponseEntity.ok(documentService.getAllDocuments());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getDocument(@PathVariable Long id) {
        try {
            Document document = documentService.getDocumentById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Document not found"));
            
            return ResponseEntity.ok(Map.of(
                "id", document.getId(),
                "fileName", document.getFileName(),
                "uploadedBy", document.getUploadedBy(),
                "uploadedAt", document.getUploadedAt(),
                "summary", document.getSummary()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}/summary")
    public ResponseEntity<Map<String, Object>> getDocumentSummary(@PathVariable Long id) {
        try {
            Document document = documentService.getDocumentById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Document not found"));
            
            return ResponseEntity.ok(Map.of(
                "documentId", id,
                "fileName", document.getFileName(),
                "summary", document.getSummary()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
