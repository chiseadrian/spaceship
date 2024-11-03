package com.example.spaceship.application.exceptions;

public class ShipDeletionException extends RuntimeException {
    public ShipDeletionException(String message, Throwable cause) {
        super(message, cause);
    }
}