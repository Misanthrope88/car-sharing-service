package mate.academy.carsharingservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mate.academy.carsharingservice.dto.CreatePaymentRequestDto;
import mate.academy.carsharingservice.dto.PaymentCancelResponseDto;
import mate.academy.carsharingservice.dto.PaymentDto;
import mate.academy.carsharingservice.model.User;
import mate.academy.carsharingservice.service.PaymentService;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Payment operations")
public class PaymentController {
    private final PaymentService paymentService;

    @Operation(summary = "Get payments with optional user filter")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of payments returned"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "User filter requires manager role")
    })
    @GetMapping
    public Page<PaymentDto> getAll(
            @Parameter(description = "Filter by user ID; available only to managers")
            @RequestParam(name = "user_id", required = false) Long userId,
            @ParameterObject Pageable pageable,
            @Parameter(hidden = true) @AuthenticationPrincipal User user) {
        return paymentService.getAll(user, userId, pageable);
    }

    @Operation(summary = "Create a Stripe payment session for a rental")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Payment session created"),
            @ApiResponse(responseCode = "400", description = "Invalid payment data"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Rental not found"),
            @ApiResponse(responseCode = "409",
                    description = "Payment already completed or fine is not applicable"),
            @ApiResponse(responseCode = "502", description = "Payment provider error")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentDto createPaymentSession(
            @RequestBody @Valid CreatePaymentRequestDto request,
            @Parameter(hidden = true) @AuthenticationPrincipal User user) {
        return paymentService.createPaymentSession(request, user);
    }

    @Operation(summary = "Handle successful Stripe payment redirect")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment marked as paid"),
            @ApiResponse(responseCode = "404", description = "Payment not found"),
            @ApiResponse(responseCode = "409", description = "Payment is not completed"),
            @ApiResponse(responseCode = "502", description = "Payment provider error")
    })
    @GetMapping("/success")
    public PaymentDto handleSuccess(@RequestParam("session_id") String sessionId) {
        return paymentService.handleSuccess(sessionId);
    }

    @Operation(summary = "Handle canceled Stripe payment redirect")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment cancellation info returned"),
            @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    @GetMapping("/cancel")
    public PaymentCancelResponseDto handleCancel(@RequestParam("session_id") String sessionId) {
        return paymentService.handleCancel(sessionId);
    }
}
