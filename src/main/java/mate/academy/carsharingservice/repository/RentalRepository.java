package mate.academy.carsharingservice.repository;

import java.util.Optional;
import mate.academy.carsharingservice.model.Rental;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RentalRepository extends JpaRepository<Rental, Long> {
    @EntityGraph(attributePaths = {"car", "user"})
    @Query("SELECT r FROM Rental r")
    Page<Rental> findAllWithDetails(Pageable pageable);

    @EntityGraph(attributePaths = {"car", "user"})
    Page<Rental> findAllByUserId(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"car", "user"})
    @Query("SELECT r FROM Rental r WHERE r.id = :id")
    Optional<Rental> findWithDetailsById(@Param("id") Long id);
}
