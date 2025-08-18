package AiBot.example.AiBot.service;

import AiBot.example.AiBot.model.Document;
import AiBot.example.AiBot.repository.DocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class DocumentService {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private FileUtil fileUtil;

    @Autowired
    private ChatService chatService;

    @Autowired(required = false)
    private MockRagService mockRagService;

    @Autowired(required = false)
    private RealRagService realRagService;

    @Autowired(required = false)
    private VectorRagService vectorRagService;

    @Autowired(required = false)
    private FallbackRagService fallbackRagService;

    @Value("${file.upload-dir}")
    private String uploadDir;

    public Document uploadDocument(MultipartFile file, String uploadedBy) throws IOException {
        // Validate file
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        if (uploadedBy == null || uploadedBy.trim().isEmpty()) {
            throw new IllegalArgumentException("UploadedBy cannot be null or empty");
        }

        System.out.println("Uploading file: " + file.getOriginalFilename() + " by user: " + uploadedBy);
        System.out.println("Upload directory: " + uploadDir);

        // Save file to local storage
        String filePath = fileUtil.saveFile(file, uploadDir);
        System.out.println("File saved to: " + filePath);
        
        // Extract text content
        String content = fileUtil.extractTextContent(file);
        System.out.println("Content extracted, length: " + (content != null ? content.length() : 0));

        // Create document entity
        Document document = new Document(file.getOriginalFilename(), filePath, uploadedBy);
        document.setContent(content);

        // Save to database
        Document savedDocument = documentRepository.save(document);
        System.out.println("Document saved to database with ID: " + savedDocument.getId());

        // Process document with RAG pipeline
        try {
            String documentId = savedDocument.getId().toString();
            
            // Debug: Log which RAG service is available
            System.out.println("=== RAG Service Debug ===");
            System.out.println("vectorRagService: " + (vectorRagService != null ? "AVAILABLE" : "NULL"));
            System.out.println("fallbackRagService: " + (fallbackRagService != null ? "AVAILABLE" : "NULL"));
            System.out.println("realRagService: " + (realRagService != null ? "AVAILABLE" : "NULL"));
            System.out.println("mockRagService: " + (mockRagService != null ? "AVAILABLE" : "NULL"));
            
            // Use appropriate RAG service (prefer vector-based RAG with OpenAI)
            if (vectorRagService != null) {
                System.out.println("Using VectorRagService for document processing");
                vectorRagService.processDocument(content, documentId);
            } else if (fallbackRagService != null) {
                System.out.println("Using FallbackRagService for document processing");
                fallbackRagService.processDocument(content, documentId);
            } else if (realRagService != null) {
                System.out.println("Using RealRagService for document processing");
                realRagService.processDocument(content, documentId);
            } else if (mockRagService != null) {
                System.out.println("Using MockRagService for document processing");
                mockRagService.processDocument(content, documentId);
            }
            
            // Generate summary using AI
            String summary = generateSummary(content);
            savedDocument.setSummary(summary);
            documentRepository.save(savedDocument);
        } catch (Exception e) {
            // Log error but don't fail the upload
            System.err.println("Failed to process document with RAG: " + e.getMessage());
        }

        return savedDocument;
    }

    public List<Document> getAllDocuments() {
        return documentRepository.findAll();
    }

    public Optional<Document> getDocumentById(Long id) {
        return documentRepository.findById(id);
    }

    public String generateSummary(String content) {
        if (content == null || content.trim().isEmpty()) {
            return "No content available for summarization.";
        }

        // Limit content length to avoid token limits
        String limitedContent = content.length() > 4000 ? content.substring(0, 4000) + "..." : content;

        String promptText = String.format(
            "Please provide a concise summary of the following document content in 2-3 sentences:\n\n%s",
            limitedContent
        );

        try {
            String response = chatService.generateResponse(promptText);
            return response != null ? response.trim() : "Summary generation failed.";
        } catch (Exception e) {
            return "Summary generation failed: " + e.getMessage();
        }
    }

    public String getDocumentContent(Long documentId) throws IOException {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found"));

        if (document.getContent() != null && !document.getContent().isEmpty()) {
            return document.getContent();
        }

        // Fallback to reading from file
        return fileUtil.readTextFile(document.getFilePath());
    }

    public String askQuestionAboutDocument(String question, Long documentId) {
        try {
            String documentIdStr = documentId.toString();
            
            // Debug: Log which RAG service is available for questions
            System.out.println("=== RAG Service Debug (Question) ===");
            System.out.println("vectorRagService: " + (vectorRagService != null ? "AVAILABLE" : "NULL"));
            System.out.println("fallbackRagService: " + (fallbackRagService != null ? "AVAILABLE" : "NULL"));
            System.out.println("realRagService: " + (realRagService != null ? "AVAILABLE" : "NULL"));
            System.out.println("mockRagService: " + (mockRagService != null ? "AVAILABLE" : "NULL"));
            
            // Use appropriate RAG service (prefer vector-based RAG with OpenAI)
            if (vectorRagService != null) {
                System.out.println("Using VectorRagService for question answering");
                return vectorRagService.askQuestion(question, documentIdStr);
            } else if (fallbackRagService != null) {
                System.out.println("Using FallbackRagService for question answering");
                return fallbackRagService.askQuestion(question, documentIdStr);
            } else if (realRagService != null) {
                System.out.println("Using RealRagService for question answering");
                return realRagService.askQuestion(question, documentIdStr);
            } else if (mockRagService != null) {
                System.out.println("Using MockRagService for question answering");
                return mockRagService.askQuestion(question, documentIdStr);
            } else {
                return "RAG service not available.";
            }
        } catch (Exception e) {
            return "Error processing question: " + e.getMessage();
        }
    }
}
