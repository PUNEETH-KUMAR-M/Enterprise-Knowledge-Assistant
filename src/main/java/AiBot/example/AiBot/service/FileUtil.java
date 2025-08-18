package AiBot.example.AiBot.service;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Component
public class FileUtil {

    public String saveFile(MultipartFile file, String uploadDir) throws IOException {
        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            String originalFilename = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String uniqueFilename = UUID.randomUUID().toString() + fileExtension;
            Path filePath = uploadPath.resolve(uniqueFilename);
            Files.copy(file.getInputStream(), filePath);
            return filePath.toString();
        } catch (Exception e) {
            throw new IOException("Failed to save file: " + e.getMessage(), e);
        }
    }

    public String readTextFile(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    public boolean isTextFile(String filename) {
        if (filename == null) return false;
        String lowerFilename = filename.toLowerCase();
        return lowerFilename.endsWith(".txt") ||
               lowerFilename.endsWith(".md") ||
               lowerFilename.endsWith(".json") ||
               lowerFilename.endsWith(".xml") ||
               lowerFilename.endsWith(".csv") ||
               lowerFilename.endsWith(".log");
    }

    public boolean isPdfFile(String filename) {
        if (filename == null) return false;
        return filename.toLowerCase().endsWith(".pdf");
    }

    public String extractTextContent(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("File is empty");
        }

        String filename = file.getOriginalFilename();
        
        if (isPdfFile(filename)) {
            return extractTextFromPdf(file);
        } else if (isTextFile(filename)) {
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        } else {
            // Try to read as text anyway
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        }
    }

    private String extractTextFromPdf(MultipartFile file) throws IOException {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        } catch (Exception e) {
            throw new IOException("Failed to extract text from PDF: " + e.getMessage(), e);
        }
    }
}
