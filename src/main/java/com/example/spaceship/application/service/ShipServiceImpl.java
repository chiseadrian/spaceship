package com.example.spaceship.application.service;

import com.example.spaceship.application.exceptions.*;
import com.example.spaceship.domain.entities.Ship;
import com.example.spaceship.domain.ports.ShipRepository;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.util.Optional;
import java.util.function.Supplier;

@Service
@Validated
@Slf4j
public class ShipServiceImpl implements ShipService {

    @Autowired
    private ShipRepository shipRepository;

    @Transactional(readOnly = true)
    @Cacheable(value = "spaceships", key = "#pageable")
    public Page<Ship> getAllShips(Pageable pageable) {
        return executeWithExceptionHandling(
                () -> {
                    Page<Ship> shipsPage = shipRepository.findAll(pageable);
                    logIfEmpty(shipsPage, "No ships found in the database");
                    return shipsPage;
                },
                "Error occurred while retrieving all ships",
                ShipRetrievalException::new
        );
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "spaceships", key = "#name + '-' + #pageable")
    public Page<Ship> findShipsByName(String name, Pageable pageable) {
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("Name parameter cannot be empty");
        }

        return executeWithExceptionHandling(
                () -> {
                    Page<Ship> shipsPage = shipRepository.findByNameContaining(name.trim(), pageable);
                    logIfEmpty(shipsPage, "No ships found with name containing: '" + name + "'");
                    return shipsPage;
                },
                "Error occurred while searching for ships",
                ShipSearchException::new
        );
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "spaceship", key = "#id")
    public Ship getShipById(Long id) {
        validateIdParameter(id);
        return executeWithExceptionHandling(
                () -> shipRepository.findById(id).orElseThrow(() -> new ShipNotFoundException("Ship not found with id: " + id)),
                "Error occurred while retrieving ship",
                ShipRetrievalException::new
        );
    }

    @Transactional
    @CacheEvict(value = {"spaceships", "spaceship"}, allEntries = true)
    public Ship createShip(@Valid Ship ship) {
        validateNewShip(ship);
        return executeWithExceptionHandling(
                () -> {
                    Ship savedShip = shipRepository.save(ship);
                    log.info("Ship created successfully with ID: {}", savedShip.getId());
                    return savedShip;
                },
                "Unexpected error creating ship",
                ShipCreationException::new
        );
    }

    @Transactional
    @CacheEvict(value = {"spaceships", "spaceship"}, allEntries = true)
    public void deleteShip(Long id) {
        validateIdParameter(id);
        executeWithExceptionHandling(
                () -> shipRepository.deleteById(id),
                "Error occurred while deleting ship",
                (message, cause) -> {
                    if (cause instanceof EmptyResultDataAccessException) {
                        return new ShipNotFoundException("Ship not found with id: " + id);
                    } else if (cause instanceof DataIntegrityViolationException) {
                        return new ShipDeletionException("Cannot delete ship due to data integrity constraints", cause);
                    }
                    return new ShipDeletionException(message, cause);
                }
        );
    }

    @Transactional
    @CacheEvict(value = {"spaceships", "spaceship"}, allEntries = true)
    public void deleteAll() {
        executeWithExceptionHandling(
                () -> {
                    long count = shipRepository.count();
                    shipRepository.deleteAll();
                    log.info("Successfully deleted {} ships", count);
                },
                "Error occurred while attempting to delete all ships",
                ShipDeletionException::new
        );
    }

    @Transactional
    @CacheEvict(value = {"spaceships", "spaceship"}, allEntries = true)
    public Ship updateShip(Long id, @Valid Ship updatedShip) {
        validateIdParameter(id);
        return executeWithExceptionHandling(
                () -> shipRepository.findById(id)
                        .map(existingShip -> {
                            updateShipFields(existingShip, updatedShip);
                            Ship savedShip = shipRepository.save(existingShip);
                            log.info("Successfully updated ship with id: {}", id);
                            return savedShip;
                        })
                        .orElseThrow(() -> new ShipNotFoundException("Ship not found with id: " + id)),
                "Error occurred while updating ship",
                ShipUpdateException::new
        );
    }

    private <T> T executeWithExceptionHandling(Supplier<T> action, String errorMessage, ExceptionSupplier exceptionSupplier) {
        try {
            return action.get();
        } catch (DataAccessException e) {
            log.error(errorMessage, e);
            throw exceptionSupplier.get(errorMessage, e);
        } catch (ShipNotFoundException | IllegalArgumentException e) {
            log.error(errorMessage, e);
            throw e;
        } catch (Exception e) {
            log.error(errorMessage, e);
            throw exceptionSupplier.get("Unexpected " + errorMessage.toLowerCase(), e);
        }
    }

    private void executeWithExceptionHandling(Runnable action, String errorMessage, ExceptionSupplier exceptionSupplier) {
        executeWithExceptionHandling(() -> { action.run(); return null; }, errorMessage, exceptionSupplier);
    }

    private void validateIdParameter(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Ship id cannot be null");
        }
    }

    private void validateNewShip(Ship ship) {
        if (ship == null) {
            throw new IllegalArgumentException("Ship cannot be null");
        }
        if (ship.getId() != null) {
            throw new IllegalArgumentException("The ID cannot be specified, it will be generated automatically");
        }
    }

    private void updateShipFields(Ship existingShip, Ship updatedShip) {
        Optional.ofNullable(updatedShip.getName()).ifPresent(existingShip::setName);
        Optional.ofNullable(updatedShip.getType()).ifPresent(existingShip::setType);
    }

    private void logIfEmpty(Page<?> page, String message) {
        if (page.isEmpty()) {
            log.info(message);
        }
    }

    @FunctionalInterface
    private interface ExceptionSupplier {
        RuntimeException get(String message, Throwable cause);
    }
}