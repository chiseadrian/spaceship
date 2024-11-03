package com.example.spaceship.application.service;

import com.example.spaceship.application.exceptions.ShipNotFoundException;
import com.example.spaceship.domain.entities.Ship;
import com.example.spaceship.domain.ports.ShipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class ShipServiceImplTest {

    private Ship testShip;

    private Pageable pageable;

    @Mock
    private ShipRepository shipRepository;

    @InjectMocks
    private ShipServiceImpl shipServiceImpl;

    @BeforeEach
    void setUp() {
        testShip = generateShip();
        pageable = Pageable.unpaged();
    }

    @Test
    void getAllShips_ShouldReturnPageOfShips() {
        Page<Ship> shipPage = new PageImpl<>(List.of(testShip));
        when(shipRepository.findAll(any(Pageable.class))).thenReturn(shipPage);

        Page<Ship> result = shipServiceImpl.getAllShips(pageable);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void findShipsByName_ShouldReturnMatchingShips() {
        Page<Ship> shipPage = new PageImpl<>(List.of(testShip));
        when(shipRepository.findByNameContaining(anyString(), any(Pageable.class))).thenReturn(shipPage);

        Page<Ship> result = shipServiceImpl.findShipsByName("Test", pageable);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getShipById_ShouldReturnShip() {
        when(shipRepository.findById(anyLong())).thenReturn(Optional.of(testShip));

        Ship result = shipServiceImpl.getShipById(1L);

        assertNotNull(result);
        assertEquals(testShip.getId(), result.getId());
    }

    @Test
    void getShipById_ShouldThrowException_WhenShipNotFound() {
        when(shipRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(ShipNotFoundException.class, () -> shipServiceImpl.getShipById(1L));
    }

    @Test
    void createShip_ShouldReturnCreatedShip() {
        when(shipRepository.save(any(Ship.class))).thenReturn(testShip);

        Ship newShip = new Ship();
        newShip.setName("New Ship");
        newShip.setType("Destroyer");

        Ship result = shipServiceImpl.createShip(newShip);

        assertNotNull(result);
        assertEquals(testShip.getId(), result.getId());
    }

    @Test
    void deleteShip_ShouldDeleteShip() {
        doNothing().when(shipRepository).deleteById(anyLong());

        assertDoesNotThrow(() -> shipServiceImpl.deleteShip(1L));

        verify(shipRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteAll_ShouldDeleteAllShips() {
        when(shipRepository.count()).thenReturn(5L);
        doNothing().when(shipRepository).deleteAll();

        assertDoesNotThrow(() -> shipServiceImpl.deleteAll());

        verify(shipRepository, times(1)).deleteAll();
    }

    @Test
    void updateShip_ShouldReturnUpdatedShip() {
        when(shipRepository.findById(anyLong())).thenReturn(Optional.of(testShip));
        when(shipRepository.save(any(Ship.class))).thenReturn(testShip);

        Ship updatedShip = new Ship();
        updatedShip.setName("Updated Ship");

        Ship result = shipServiceImpl.updateShip(1L, updatedShip);

        assertNotNull(result);
        assertEquals("Updated Ship", result.getName());
    }

    @Test
    void updateShip_ShouldThrowException_WhenShipNotFound() {
        when(shipRepository.findById(anyLong())).thenReturn(Optional.empty());

        Ship updatedShip = new Ship();
        updatedShip.setName("Updated Ship");

        assertThrows(ShipNotFoundException.class, () -> shipServiceImpl.updateShip(1L, updatedShip));
    }

    private Ship generateShip() {
        Ship ship = new Ship();
        ship.setId(1L);
        ship.setName("Test Ship");
        ship.setType("Cruiser");
        return ship;
    }
}