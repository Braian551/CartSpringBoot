package com.example.cart.exception;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.validation.ConstraintViolationException;

/** Mantiene respuestas JSON simples para el endpoint REST, sin HTML MVC. */
@RestControllerAdvice(assignableTypes = com.example.cart.controller.AdminCartRestController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CartRestExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(CartRestExceptionHandler.class);

    @ExceptionHandler(CartNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound() {
        log.warn("REST cart not found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("success", false, "message", "Carrito no encontrado"));
    }

    @ExceptionHandler(InvalidCartRequestException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest() {
        log.warn("Invalid REST cart request");
        return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "La consulta del carrito no es válida"));
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            BindException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            HttpMessageNotReadableException.class,
            ConstraintViolationException.class
    })
    public ResponseEntity<Map<String, Object>> handleFrameworkBadRequest() {
        log.warn("Invalid REST cart request parameters");
        return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "La consulta del carrito no es válida"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception exception) {
        log.error("Unexpected REST error while consulting carts", exception);
        return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "No fue posible consultar los carritos"));
    }
}
