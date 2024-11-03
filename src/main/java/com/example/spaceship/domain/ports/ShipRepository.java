package com.example.spaceship.domain.ports;

import com.example.spaceship.domain.entities.Ship;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;


public interface ShipRepository {
    Page<Ship> findAll(Pageable pageable);
    Page<Ship> findByNameContaining(String name, Pageable pageable);
    Optional<Ship> findById(Long id);
    Ship save(Ship nave);
    void deleteById(Long id);
    void deleteAll();
    long count();
}
