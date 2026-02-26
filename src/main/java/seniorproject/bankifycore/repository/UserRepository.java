package seniorproject.bankifycore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import seniorproject.bankifycore.domain.User;
import seniorproject.bankifycore.domain.enums.UserRole;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository  extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByRole(UserRole role);
}
