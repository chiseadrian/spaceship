package com.example.spaceship.infrastructure.database;

import com.example.spaceship.domain.entities.Ship;
import com.example.spaceship.domain.ports.ShipRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShipRepositoryImpl extends JpaRepository<Ship, Long>, ShipRepository {
}