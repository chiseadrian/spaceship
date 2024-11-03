package com.example.spaceship.application.controller;

import com.example.spaceship.application.exceptions.ShipAlreadyExistsException;
import com.example.spaceship.application.exceptions.ShipNotFoundException;
import com.example.spaceship.application.service.ShipService;
import com.example.spaceship.application.util.UriBuilder;
import com.example.spaceship.domain.entities.Ship;
import com.example.spaceship.infrastructure.kakfa.KafkaConsumer;
import com.example.spaceship.infrastructure.kakfa.KafkaProducer;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.function.Supplier;

@RestController
@RequestMapping("/api/spaceships")
@Tag(name = "Ship", description = "Ship management APIs")
@Slf4j
public class ShipController {

    @Autowired
    private ShipService shipService;

    @Autowired
    private UriBuilder uriBuilder;

    @Autowired
    private KafkaProducer kafkaProducer;

    @Autowired
    private KafkaConsumer kafkaConsumer;

    @GetMapping
    @Operation(summary = "Get all ships", description = "Retrieves a paginated list of all ships with sorting options")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved the list of ships"),
            @ApiResponse(responseCode = "400", description = "Invalid pagination or sorting parameters"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Page<Ship>> getAllShips(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(value = "page", defaultValue = "0") int page,
            @Parameter(description = "Number of items per page", example = "10")
            @RequestParam(value = "size", defaultValue = "10") int size,
            @Parameter(description = "Field to sort by", example = "name")
            @RequestParam(value = "sort", defaultValue = "name") String sort,
            @Parameter(description = "Sort direction (asc or desc)", example = "asc")
            @RequestParam(value = "direction", defaultValue = "asc") String direction
    ) {
        return executeWithExceptionHandling(() -> {
            Pageable pageable = createPageable(page, size, sort, direction);
            return ResponseEntity.ok(shipService.getAllShips(pageable));
        });
    }

    @GetMapping("/kafka")
    @Operation(summary = "Get all ships from Kafka", description = "Retrieves all ships received from the Kafka topic")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved the list of ships"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<Ship>> getAllShipsFromKafka() {
        List<Ship> ships = kafkaConsumer.getAllShips();
        return executeWithExceptionHandling(() -> ResponseEntity.ok(ships));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a ship by its ID", description = "Retrieves a ship based on the provided ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved the ship"),
            @ApiResponse(responseCode = "404", description = "Ship not found"),
            @ApiResponse(responseCode = "400", description = "Invalid ID supplied"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Ship> getShipById(@PathVariable Long id) {
        return executeWithExceptionHandling(() -> ResponseEntity.ok(shipService.getShipById(id)));
    }

    @GetMapping("/search")
    @Operation(summary = "Search ships by name", description = "Retrieves a paginated list of ships whose names contain the given search term")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved the list of ships"),
            @ApiResponse(responseCode = "400", description = "Invalid search term or pagination parameters"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Page<Ship>> findShipsByName(
            @Parameter(description = "Name to search for", required = true, example = "Enterprise")
            @RequestParam String name,
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Field to sort by", example = "name")
            @RequestParam(defaultValue = "name") String sort,
            @Parameter(description = "Sort direction (asc or desc)", example = "asc")
            @RequestParam(defaultValue = "asc") String direction
    ) {
        if (!StringUtils.hasText(name)) {
            return ResponseEntity.badRequest().build();
        }
        return executeWithExceptionHandling(() -> {
            Pageable pageable = createPageable(page, size, sort, direction);
            return ResponseEntity.ok(shipService.findShipsByName(name, pageable));
        });
    }

    @PostMapping
    @Operation(summary = "Create a new ship", description = "Creates a new ship with the provided details")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Ship created successfully", content = @Content(schema = @Schema(implementation = Ship.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "409", description = "Ship already exists"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Ship> createShip(
            @Parameter(description = "Ship to be created", required = true, schema = @Schema(implementation = Ship.class))
            @Valid @RequestBody Ship ship) {
        return executeWithExceptionHandling(() -> {
            Ship createdShip = shipService.createShip(ship);
            URI location = uriBuilder.buildUri("/{id}", createdShip.getId());
            kafkaProducer.sendMessage(createdShip);
            return ResponseEntity.created(location).body(createdShip);
        });
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a ship", description = "Updates an existing ship with the provided details")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ship updated successfully", content = @Content(schema = @Schema(implementation = Ship.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "404", description = "Ship not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Ship> updateShip(
            @Parameter(description = "ID of the ship to be updated", required = true)
            @PathVariable Long id,
            @Parameter(description = "Updated ship details", required = true, schema = @Schema(implementation = Ship.class))
            @Valid @RequestBody Ship updatedShip) {
        return executeWithExceptionHandling(() -> ResponseEntity.ok(shipService.updateShip(id, updatedShip)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a ship", description = "Deletes an existing ship by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Ship successfully deleted"),
            @ApiResponse(responseCode = "404", description = "Ship not found"),
            @ApiResponse(responseCode = "400", description = "Invalid ship ID supplied"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Void> deleteShipById(
            @Parameter(description = "ID of the ship to be deleted", required = true)
            @PathVariable Long id) {
        return executeWithExceptionHandling(() -> {
            shipService.deleteShip(id);
            return ResponseEntity.noContent().build();
        });
    }

    @DeleteMapping
    @Operation(summary = "Delete all ships", description = "Deletes all existing ships. This operation cannot be undone.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "All ships successfully deleted"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Void> deleteAll() {
        log.warn("Received request to delete all ships");
        return executeWithExceptionHandling(() -> {
            shipService.deleteAll();
            return ResponseEntity.noContent().build();
        });
    }

    private Pageable createPageable(int page, int size, String sort, String direction) {
        Sort.Direction sortDirection = Sort.Direction.fromString(direction.toUpperCase());
        return PageRequest.of(page, size, Sort.by(sortDirection, sort));
    }

    private <T> ResponseEntity<T> executeWithExceptionHandling(Supplier<ResponseEntity<T>> action) {
        try {
            return action.get();
        } catch (ShipNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException | ShipAlreadyExistsException e) {
            return ResponseEntity.badRequest().body((T) e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error occurred", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}