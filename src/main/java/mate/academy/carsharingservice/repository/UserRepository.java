package mate.academy.carsharingservice.repository;

import java.util.Optional;
import mate.academy.carsharingservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
