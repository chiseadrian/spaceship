package com.example.spaceship.application.controller;

import com.example.spaceship.application.exceptions.ShipAlreadyExistsException;
import com.example.spaceship.application.exceptions.ShipNotFoundException;
import com.example.spaceship.application.service.ShipServiceImpl;
import com.example.spaceship.domain.entities.Ship;
import com.example.spaceship.application.util.UriBuilder;
import com.example.spaceship.infrastructure.kakfa.KafkaConsumer;
import com.example.spaceship.infrastructure.kakfa.KafkaProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ShipControllerTest {

    @Mock
    private ShipServiceImpl shipServiceImpl;

    @Mock
    private UriBuilder uriBuilder;

    @Mock
    private KafkaProducer kafkaProducer;

    @Mock
    private KafkaConsumer kafkaConsumer;

    @InjectMocks
    private ShipController shipController;

    private final Ship SHIP1 = generateShip(1L, "name1", "type1");

    private final Ship SHIP2 = generateShip(2L, "name2", "type2");

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getAllShips() {
        Page<Ship> shipPage = new PageImpl<>(List.of(SHIP1));
        when(shipServiceImpl.getAllShips(any(Pageable.class))).thenReturn(shipPage);

        ResponseEntity<Page<Ship>> response = shipController.getAllShips(0, 10, "name", "asc");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getTotalElements());
        verify(shipServiceImpl).getAllShips(any(Pageable.class));
    }

    @Test
    public void getAllShipsFromKafka() {
        List<Ship> ships = List.of(SHIP1, SHIP2);
        when(kafkaConsumer.getAllShips()).thenReturn(ships);

        ResponseEntity<List<Ship>> response = shipController.getAllShipsFromKafka();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(ships, response.getBody());
    }

    @Test
    void getShipById_ExistingId_ReturnsShip() {
        when(shipServiceImpl.getShipById(1L)).thenReturn(SHIP1);

        ResponseEntity<Ship> response = shipController.getShipById(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(SHIP1.getName(), response.getBody().getName());
        verify(shipServiceImpl).getShipById(1L);
    }

    @Test
    void getShipById_NonExistingId_ReturnsNotFound() {
        when(shipServiceImpl.getShipById(1L)).thenThrow(new ShipNotFoundException("Ship not found"));

        ResponseEntity<Ship> response = shipController.getShipById(1L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(shipServiceImpl).getShipById(1L);
    }

    @Test
    void findShipsByName_ValidName_ReturnsPageOfShips() {
        Page<Ship> shipPage = new PageImpl<>(List.of(SHIP1, SHIP2));
        when(shipServiceImpl.findShipsByName(anyString(), any(Pageable.class))).thenReturn(shipPage);

        ResponseEntity<Page<Ship>> response = shipController.findShipsByName("Test", 0, 10, "name", "asc");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(shipPage.getSize(), response.getBody().getTotalElements());
        verify(shipServiceImpl).findShipsByName(eq("Test"), any(Pageable.class));
    }

    @Test
    void createShip_ValidShip_ReturnsCreatedShip() {
        String uri = "http://test.com/api/spaceships/1";
        when(shipServiceImpl.createShip(any(Ship.class))).thenReturn(SHIP1);
        when(uriBuilder.buildUri(eq("/{id}"), eq(SHIP1.getId()))).thenReturn(URI.create(uri));
        doNothing().when(kafkaProducer).sendMessage(any(Ship.class));

        ResponseEntity<Ship> response = shipController.createShip(SHIP1);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(SHIP1.getName(), response.getBody().getName());
        assertEquals(uri, Objects.requireNonNull(response.getHeaders().getLocation()).toString());
        verify(shipServiceImpl).createShip(any(Ship.class));
        verify(uriBuilder).buildUri(eq("/{id}"), eq(SHIP1.getId()));
    }

    @Test
    void createShip_DuplicateShip_ReturnsBadRequest() {
        when(shipServiceImpl.createShip(any(Ship.class))).thenThrow(new ShipAlreadyExistsException("Ship already exists"));

        ResponseEntity<Ship> response = shipController.createShip(SHIP1);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(shipServiceImpl).createShip(any(Ship.class));
    }

    @Test
    void updateShip_ExistingShip_ReturnsUpdatedShip() {
        when(shipServiceImpl.updateShip(eq(SHIP1.getId()), any(Ship.class))).thenReturn(SHIP1);

        ResponseEntity<Ship> response = shipController.updateShip(SHIP1.getId(), SHIP1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(SHIP1.getName(), response.getBody().getName());
        verify(shipServiceImpl).updateShip(eq(SHIP1.getId()), any(Ship.class));
    }

    @Test
    void updateShip_NonExistingShip_ReturnsNotFound() {
        when(shipServiceImpl.updateShip(eq(SHIP1.getId()), any(Ship.class))).thenThrow(new ShipNotFoundException("Ship not found"));

        ResponseEntity<Ship> response = shipController.updateShip(SHIP1.getId(), SHIP1);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(shipServiceImpl).updateShip(eq(SHIP1.getId()), any(Ship.class));
    }

    @Test
    void deleteShipById_ExistingId_ReturnsNoContent() {
        doNothing().when(shipServiceImpl).deleteShip(1L);

        ResponseEntity<Void> response = shipController.deleteShipById(1L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(shipServiceImpl).deleteShip(1L);
    }

    @Test
    void deleteShipById_NonExistingId_ReturnsNotFound() {
        doThrow(new ShipNotFoundException("Ship not found")).when(shipServiceImpl).deleteShip(1L);

        ResponseEntity<Void> response = shipController.deleteShipById(1L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(shipServiceImpl).deleteShip(1L);
    }

    @Test
    void deleteAll_ReturnsNoContent() {
        doNothing().when(shipServiceImpl).deleteAll();

        ResponseEntity<Void> response = shipController.deleteAll();

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(shipServiceImpl).deleteAll();
    }

    private Ship generateShip(Long id, String name, String type) {
        Ship ship = new Ship();
        ship.setId(id);
        ship.setName(name);
        ship.setType(type);
        return ship;
    }
}