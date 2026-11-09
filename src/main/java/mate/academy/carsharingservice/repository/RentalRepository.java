package mate.academy.carsharingservice.repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import mate.academy.carsharingservice.model.Rental;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RentalRepository extends JpaRepository<Rental, Long>,
        JpaSpecificationExecutor<Rental> {
    @Override
    @EntityGraph(attributePaths = {"car", "user"})
    Page<Rental> findAll(Specification<Rental> specification, Pageable pageable);

    @EntityGraph(attributePaths = {"car", "user"})
    @Query("SELECT r FROM Rental r WHERE r.id = :id")
    Optional<Rental> findWithDetailsById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Rental r WHERE r.id = :id")
    Optional<Rental> findByIdForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = {"car", "user"})
    List<Rental> findAllByActualReturnDateIsNullAndReturnDateLessThanEqual(LocalDate date);
}
