package com.example.spaceship.application.exceptions;

public class ShipUpdateException extends RuntimeException {
    public ShipUpdateException(String message, Throwable cause) {
        super(message, cause);
    }
}