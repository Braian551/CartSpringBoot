package com.example.cart.exception;

public class CartNotFoundException extends RuntimeException {

    public CartNotFoundException(Integer id) {
        super("Cart not found: " + id);
    }
}
