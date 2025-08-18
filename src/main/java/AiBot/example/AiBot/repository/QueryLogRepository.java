package AiBot.example.AiBot.repository;

import AiBot.example.AiBot.model.QueryLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QueryLogRepository extends JpaRepository<QueryLog, Long> {
    // For analytics, you could later add:
    List<QueryLog> findByUsernameOrderByTimestampDesc(String username);
}
