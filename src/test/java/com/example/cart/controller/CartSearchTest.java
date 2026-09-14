package com.example.cart.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;

import com.example.cart.config.CartProperties;
import com.example.cart.entity.Cart;
import com.example.cart.exception.GlobalExceptionHandler;
import com.example.cart.repository.CartRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class CartSearchTest {

    private CartRepository cartRepository;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        cartRepository = mock(CartRepository.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new CartController(cartRepository, new CartProperties()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void searchesTwoFieldsWithAndAndKeepsPagination() throws Exception {
        when(cartRepository.findByUserIdContainingIgnoreCaseAndSessionIdContainingIgnoreCase(
                eq("user-1"), eq("session-1"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/cart")
                        .param("userId", " user-1 ")
                        .param("sessionId", "session-1")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(view().name("cart/list"));

        verify(cartRepository).findByUserIdContainingIgnoreCaseAndSessionIdContainingIgnoreCase(
                eq("user-1"), eq("session-1"), any(Pageable.class));
        verify(cartRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void searchesThreeFieldsWithOrIncludingNumericId() throws Exception {
        when(cartRepository.findByUserIdContainingIgnoreCaseOrSessionIdContainingIgnoreCaseOrId(
                eq("42"), eq("42"), eq(42), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/cart").param("orSearch", "42").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(view().name("cart/list"));

        verify(cartRepository).findByUserIdContainingIgnoreCaseOrSessionIdContainingIgnoreCaseOrId(
                eq("42"), eq("42"), eq(42), any(Pageable.class));
    }

    @Test
    void keepsNonNumericGlobalSearchAsAParameterizedOrValue() throws Exception {
        String search = "' OR '1'='1";
        when(cartRepository.findByUserIdContainingIgnoreCaseOrSessionIdContainingIgnoreCaseOrId(
                eq(search), eq(search), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/cart").param("orSearch", search))
                .andExpect(status().isOk())
                .andExpect(view().name("cart/list"));

        verify(cartRepository).findByUserIdContainingIgnoreCaseOrSessionIdContainingIgnoreCaseOrId(
                eq(search), eq(search), isNull(), any(Pageable.class));
    }
}
