package mate.academy.carsharingservice.service.impl;

import lombok.RequiredArgsConstructor;
import mate.academy.carsharingservice.dto.PaymentDto;
import mate.academy.carsharingservice.mapper.PaymentMapper;
import mate.academy.carsharingservice.model.Payment;
import mate.academy.carsharingservice.model.Role;
import mate.academy.carsharingservice.model.User;
import mate.academy.carsharingservice.repository.PaymentRepository;
import mate.academy.carsharingservice.service.PaymentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentDto> getAll(User user, Long userId, Pageable pageable) {
        if (user.getRole() != Role.MANAGER && userId != null) {
            throw new AccessDeniedException("User filter requires manager role");
        }
        Page<Payment> payments;
        if (user.getRole() == Role.MANAGER) {
            payments = userId == null
                    ? paymentRepository.findAll(pageable)
                    : paymentRepository.findAllByRentalUserId(userId, pageable);
        } else {
            payments = paymentRepository.findAllByRentalUserId(user.getId(), pageable);
        }
        return payments.map(paymentMapper::toDto);
    }
}
