package com.example.spaceship.application.exceptions;

public class ShipCreationException extends RuntimeException {
    public ShipCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}