package com.example.cart.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import com.example.cart.config.CartProperties;
import com.example.cart.entity.Cart;
import com.example.cart.exception.CartRestExceptionHandler;
import com.example.cart.repository.CartRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AdminCartRestControllerTest {

    private CartRepository cartRepository;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        cartRepository = mock(CartRepository.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new AdminCartRestController(cartRepository, new CartProperties()))
                .setControllerAdvice(new CartRestExceptionHandler())
                .build();
    }

    @Test
    void exposesThreeFieldOrSearchAsPaginatedJson() throws Exception {
        when(cartRepository.findByUserIdContainingIgnoreCaseOrSessionIdContainingIgnoreCaseOrId(
                eq("42"), eq("42"), eq(42), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/admin/carts").param("search", "42").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagination.page").value(1))
                .andExpect(jsonPath("$.pagination.size").value(5))
                .andExpect(jsonPath("$.data").isArray());

        verify(cartRepository).findByUserIdContainingIgnoreCaseOrSessionIdContainingIgnoreCaseOrId(
                eq("42"), eq("42"), eq(42), any(Pageable.class));
    }

    @Test
    void exposesTwoFieldAndSearchAsPaginatedJson() throws Exception {
        when(cartRepository.findByUserIdContainingIgnoreCaseAndSessionIdContainingIgnoreCase(
                eq("user-1"), eq("session-1"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/admin/carts")
                        .param("userId", "user-1")
                        .param("sessionId", "session-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagination.totalItems").value(0));

        verify(cartRepository).findByUserIdContainingIgnoreCaseAndSessionIdContainingIgnoreCase(
                eq("user-1"), eq("session-1"), any(Pageable.class));
    }

    @Test
    void returnsControlledJsonForConflictingSearchModes() throws Exception {
        mockMvc.perform(get("/api/admin/carts")
                        .param("search", "42")
                        .param("userId", "user-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("La consulta del carrito no es válida"));
    }
}
