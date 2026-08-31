package com.example.cart.exception;

public class InvalidCartRequestException extends RuntimeException {

    public InvalidCartRequestException() {
        super("Invalid cart request");
    }
}
