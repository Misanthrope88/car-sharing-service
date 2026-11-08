package mate.academy.carsharingservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mate.academy.carsharingservice.dto.CreateRentalRequestDto;
import mate.academy.carsharingservice.dto.RentalResponseDto;
import mate.academy.carsharingservice.service.RentalService;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rentals")
@RequiredArgsConstructor
@Tag(name = "Rentals", description = "Rental creation and retrieval")
public class RentalController {
    private final RentalService rentalService;

    @Operation(summary = "Create a rental for the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Rental created"),
            @ApiResponse(responseCode = "400", description = "Invalid rental data"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "404", description = "Active car or user not found"),
            @ApiResponse(responseCode = "409", description = "Car inventory exhausted")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RentalResponseDto create(@RequestBody @Valid CreateRentalRequestDto request) {
        return rentalService.create(request);
    }

    @Operation(summary = "Get rentals with optional user and activity filters")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of rentals returned"),
            @ApiResponse(responseCode = "400", description = "Invalid filter value"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "User filter requires manager role")
    })
    @GetMapping
    public Page<RentalResponseDto> getAll(
            @Parameter(description = "Filter by user ID; available only to managers")
            @RequestParam(name = "user_id", required = false) Long userId,
            @Parameter(description = "True for unreturned rentals, false for returned rentals")
            @RequestParam(name = "is_active", required = false) Boolean isActive,
            @ParameterObject Pageable pageable) {
        return rentalService.getAll(userId, isActive, pageable);
    }

    @Operation(summary = "Get rental details")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rental returned"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Rental not found")
    })
    @GetMapping("/{id}")
    public RentalResponseDto getById(@PathVariable Long id) {
        return rentalService.getById(id);
    }

    @Operation(summary = "Return a rental")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rental returned"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Rental not found"),
            @ApiResponse(responseCode = "409", description = "Rental already returned")
    })
    @PostMapping("/{id}/return")
    public RentalResponseDto returnRental(@PathVariable Long id) {
        return rentalService.returnRental(id);
    }
}
