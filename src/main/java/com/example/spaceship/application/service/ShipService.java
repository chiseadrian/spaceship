package com.example.spaceship.application.service;

import com.example.spaceship.domain.entities.Ship;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.validation.Valid;

public interface ShipService {
    Page<Ship> getAllShips(Pageable pageable);
    Page<Ship> findShipsByName(String name, Pageable pageable);
    Ship getShipById(Long id);
    Ship createShip(@Valid Ship ship);
    void deleteShip(Long id);
    void deleteAll();
    Ship updateShip(Long id, @Valid Ship updatedShip);
}
