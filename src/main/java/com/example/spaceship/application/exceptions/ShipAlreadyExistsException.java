package com.example.spaceship.application.exceptions;

public class ShipAlreadyExistsException extends RuntimeException {
    public ShipAlreadyExistsException(String message) {
        super(message);
    }
}
