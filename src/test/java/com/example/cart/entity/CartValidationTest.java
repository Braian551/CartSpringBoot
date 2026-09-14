package com.example.cart.entity;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CartValidationTest {

    private static jakarta.validation.ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void acceptsOptionalIdentifiersAndAutomaticDates() {
        Cart cart = new Cart();

        assertTrue(validator.validate(cart).isEmpty());
    }

    @Test
    void rejectsOversizedAndControlCharacterIdentifiers() {
        Cart cart = new Cart();
        cart.setUserId("u".repeat(51));
        cart.setSessionId("session\u0000id");

        var violations = validator.validate(cart);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("userId")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("sessionId")));
    }

    @Test
    void rejectsDatesInTheFuture() {
        Cart cart = new Cart();
        cart.setUpdatedAt(LocalDateTime.now().plusMinutes(1));

        assertTrue(validator.validate(cart).stream()
                .anyMatch(v -> v.getMessage().contains("fechas del carrito")));
    }
}
