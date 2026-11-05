package mate.academy.carsharingservice.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import mate.academy.carsharingservice.model.Car;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CarRepository extends JpaRepository<Car, Long> {
    @Query("SELECT c FROM Car c WHERE c.isDeleted = false")
    Page<Car> findAllActive(Pageable pageable);

    @Query("SELECT c FROM Car c WHERE c.id = :id AND c.isDeleted = false")
    Optional<Car> findActiveById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Car c WHERE c.id = :id AND c.isDeleted = false")
    Optional<Car> findActiveByIdForUpdate(@Param("id") Long id);
}
