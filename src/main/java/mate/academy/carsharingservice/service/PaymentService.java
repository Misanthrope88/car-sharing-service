package mate.academy.carsharingservice.service;

import mate.academy.carsharingservice.dto.CreatePaymentRequestDto;
import mate.academy.carsharingservice.dto.PaymentCancelResponseDto;
import mate.academy.carsharingservice.dto.PaymentDto;
import mate.academy.carsharingservice.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PaymentService {
    Page<PaymentDto> getAll(User user, Long userId, Pageable pageable);

    PaymentDto createPaymentSession(CreatePaymentRequestDto request, User user);

    PaymentDto handleSuccess(String sessionId);

    PaymentCancelResponseDto handleCancel(String sessionId);
}
