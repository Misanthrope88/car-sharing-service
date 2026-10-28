package mate.academy.carsharingservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mate.academy.carsharingservice.dto.CarDto;
import mate.academy.carsharingservice.dto.CreateCarRequestDto;
import mate.academy.carsharingservice.service.CarService;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cars")
@RequiredArgsConstructor
@Tag(name = "Cars", description = "Car catalog operations")
public class CarController {
    private final CarService carService;

    @Operation(summary = "Get all cars")
    @ApiResponse(responseCode = "200", description = "Page of cars returned")
    @GetMapping
    public Page<CarDto> getAll(@ParameterObject Pageable pageable) {
        return carService.getAll(pageable);
    }

    @Operation(summary = "Get a car by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Car returned"),
            @ApiResponse(responseCode = "404", description = "Car not found")
    })
    @GetMapping("/{id}")
    public CarDto getById(@PathVariable Long id) {
        return carService.getById(id);
    }

    @Operation(summary = "Create a car")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Car created"),
            @ApiResponse(responseCode = "400", description = "Invalid car data")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CarDto create(@RequestBody @Valid CreateCarRequestDto request) {
        return carService.create(request);
    }

    @Operation(summary = "Update a car")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Car updated"),
            @ApiResponse(responseCode = "400", description = "Invalid car data"),
            @ApiResponse(responseCode = "404", description = "Car not found")
    })
    @PutMapping("/{id}")
    public CarDto update(@PathVariable Long id,
                        @RequestBody @Valid CreateCarRequestDto request) {
        return carService.update(id, request);
    }

    @Operation(summary = "Delete a car")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Car deleted"),
            @ApiResponse(responseCode = "404", description = "Car not found")
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteById(@PathVariable Long id) {
        carService.deleteById(id);
    }
}
