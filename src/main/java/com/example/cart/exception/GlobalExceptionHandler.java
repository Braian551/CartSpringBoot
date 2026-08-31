package com.example.cart.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;

import jakarta.validation.ConstraintViolationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(CartNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ModelAndView handleCartNotFound() {
        return view("error/404", HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler({
            InvalidCartRequestException.class,
            MethodArgumentNotValidException.class,
            BindException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            HttpMessageNotReadableException.class,
            ConstraintViolationException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ModelAndView handleBadRequest() {
        log.warn("Invalid cart request");
        return view("error/400", HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ModelAndView handleUnexpectedError(Exception exception) {
        log.error("Unexpected error while processing cart request", exception);
        return view("error/500", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ModelAndView view(String viewName, HttpStatus status) {
        ModelAndView modelAndView = new ModelAndView(viewName);
        modelAndView.setStatus(status);
        return modelAndView;
    }
}
