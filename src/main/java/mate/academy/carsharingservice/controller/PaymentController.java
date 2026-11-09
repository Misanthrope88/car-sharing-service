package mate.academy.carsharingservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import mate.academy.carsharingservice.dto.PaymentDto;
import mate.academy.carsharingservice.model.User;
import mate.academy.carsharingservice.service.PaymentService;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
}
