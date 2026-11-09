package mate.academy.carsharingservice.service.impl;

import java.math.BigDecimal;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import mate.academy.carsharingservice.dto.CreatePaymentRequestDto;
import mate.academy.carsharingservice.dto.PaymentCancelResponseDto;
import mate.academy.carsharingservice.dto.PaymentDto;
import mate.academy.carsharingservice.dto.StripeSessionDto;
import mate.academy.carsharingservice.exception.EntityNotFoundException;
import mate.academy.carsharingservice.exception.PaymentException;
import mate.academy.carsharingservice.mapper.PaymentMapper;
import mate.academy.carsharingservice.model.Payment;
import mate.academy.carsharingservice.model.PaymentStatus;
import mate.academy.carsharingservice.model.PaymentType;
import mate.academy.carsharingservice.model.Rental;
import mate.academy.carsharingservice.model.Role;
import mate.academy.carsharingservice.model.User;
import mate.academy.carsharingservice.repository.PaymentRepository;
import mate.academy.carsharingservice.repository.RentalRepository;
import mate.academy.carsharingservice.service.PaymentService;
import mate.academy.carsharingservice.service.StripeService;
import mate.academy.carsharingservice.service.payment.PaymentAmountCalculatorProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private static final String SESSION_ID_PARAM = "session_id";
    private static final String SESSION_ID_PLACEHOLDER = "{CHECKOUT_SESSION_ID}";
    private static final String CANCEL_MESSAGE = "Payment was canceled. You can complete it later, "
            + "but the session is available for 24 hours.";

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final RentalRepository rentalRepository;
    private final StripeService stripeService;
    private final PaymentAmountCalculatorProvider calculatorProvider;

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

    @Override
    @Transactional
    public PaymentDto createPaymentSession(CreatePaymentRequestDto request, User user) {
        Rental rental = rentalRepository.findByIdForUpdate(request.rentalId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Can't find rental by id: " + request.rentalId()));
        if (user.getRole() != Role.MANAGER && !rental.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Access denied");
        }
        PaymentType type = request.type();
        if (type == PaymentType.FINE && (rental.getActualReturnDate() == null
                || !rental.getActualReturnDate().isAfter(rental.getReturnDate()))) {
            throw new PaymentException(
                    "Fine is available only for rentals returned after the return date");
        }
        Optional<Payment> existing = paymentRepository.findByRentalIdAndType(rental.getId(), type);
        if (existing.isPresent()) {
            if (existing.get().getStatus() == PaymentStatus.PAID) {
                throw new PaymentException("Payment is already completed");
            }
            return paymentMapper.toDto(existing.get());
        }
        BigDecimal amount = calculatorProvider.getCalculator(type).calculate(rental);
        String successUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/payments/success")
                .queryParam(SESSION_ID_PARAM, SESSION_ID_PLACEHOLDER)
                .build()
                .toUriString();
        String cancelUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/payments/cancel")
                .queryParam(SESSION_ID_PARAM, SESSION_ID_PLACEHOLDER)
                .build()
                .toUriString();
        String productName = type.name() + " for rental #" + rental.getId();
        StripeSessionDto session = stripeService.createSession(
                amount, productName, successUrl, cancelUrl);
        Payment payment = new Payment();
        payment.setStatus(PaymentStatus.PENDING);
        payment.setType(type);
        payment.setRental(rental);
        payment.setSessionUrl(session.sessionUrl());
        payment.setSessionId(session.sessionId());
        payment.setAmountToPay(amount);
        return paymentMapper.toDto(paymentRepository.save(payment));
    }

    @Override
    @Transactional
    public PaymentDto handleSuccess(String sessionId) {
        Payment payment = findBySessionId(sessionId);
        if (payment.getStatus() == PaymentStatus.PAID) {
            return paymentMapper.toDto(payment);
        }
        if (!stripeService.isSessionPaid(sessionId)) {
            throw new PaymentException("Payment is not completed");
        }
        payment.setStatus(PaymentStatus.PAID);
        return paymentMapper.toDto(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentCancelResponseDto handleCancel(String sessionId) {
        Payment payment = findBySessionId(sessionId);
        return new PaymentCancelResponseDto(CANCEL_MESSAGE, payment.getSessionUrl());
    }

    private Payment findBySessionId(String sessionId) {
        return paymentRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Can't find payment by session id: " + sessionId));
    }
}
