package AiBot.example.AiBot.repository;

import AiBot.example.AiBot.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    // You can add custom queries if needed later
}
