package com.example.spaceship.application.exceptions;

public class ShipRetrievalException extends RuntimeException {
    public ShipRetrievalException(String message, Throwable cause) {
        super(message, cause);
    }
}