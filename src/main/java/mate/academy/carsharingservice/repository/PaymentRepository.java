package mate.academy.carsharingservice.repository;

import java.util.Optional;
import mate.academy.carsharingservice.model.Payment;
import mate.academy.carsharingservice.model.PaymentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Page<Payment> findAllByRentalUserId(Long userId, Pageable pageable);

    Optional<Payment> findBySessionId(String sessionId);

    Optional<Payment> findByRentalIdAndType(Long rentalId, PaymentType type);
}
