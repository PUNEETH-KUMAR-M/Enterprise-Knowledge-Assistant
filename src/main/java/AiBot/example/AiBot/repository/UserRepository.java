package AiBot.example.AiBot.repository;

import AiBot.example.AiBot.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // Find user by username (for login/authentication)
    Optional<User> findByUsername(String username);

    // Check if username already exists (useful when registering)
    boolean existsByUsername(String username);
}
