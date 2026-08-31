package com.example.cart.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

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
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.handler;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class CartControllerSecurityTest {

    private CartRepository cartRepository;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        cartRepository = mock(CartRepository.class);
        CartProperties properties = new CartProperties();
        CartController controller = new CartController(cartRepository, properties);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void acceptsValidCreateAndDoesNotBindInternalFields() throws Exception {
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> {
            Cart incoming = invocation.getArgument(0);
            Cart persisted = new Cart();
            persisted.setId(10);
            persisted.setUserId(incoming.getUserId());
            persisted.setSessionId(incoming.getSessionId());
            return persisted;
        });

        mockMvc.perform(post("/cart")
                        .param("userId", "user-1")
                        .param("sessionId", "session-1")
                        .param("id", "999")
                        .param("createdAt", "2000-01-01T00:00:00")
                        .param("updatedAt", "2000-01-01T00:00:00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/cart"));

        var saved = org.mockito.ArgumentCaptor.forClass(Cart.class);
        verify(cartRepository).save(saved.capture());
        assertNull(saved.getValue().getId());
        assertNull(saved.getValue().getCreatedAt());
        assertNull(saved.getValue().getUpdatedAt());
        assertEquals("user-1", saved.getValue().getUserId());
        assertEquals("session-1", saved.getValue().getSessionId());
    }

    @Test
    void rejectsOversizedInputBeforePersistence() throws Exception {
        mockMvc.perform(post("/cart").param("userId", "x".repeat(51)))
                .andExpect(status().isOk())
                .andExpect(view().name("cart/create"));

        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void treatsSqlLookingValueAsData() throws Exception {
        String sqlLookingValue = "' OR '1'='1";
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/cart").param("userId", sqlLookingValue))
                .andExpect(status().is3xxRedirection());

        var saved = org.mockito.ArgumentCaptor.forClass(Cart.class);
        verify(cartRepository).save(saved.capture());
        assertEquals(sqlLookingValue, saved.getValue().getUserId());
    }

    @Test
    void rejectsNegativeIdWithBadRequest() throws Exception {
        mockMvc.perform(get("/cart/-1"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error/400"));

        verify(cartRepository, never()).findById(any());
    }

    @Test
    void rejectsNonNumericIdWithBadRequest() throws Exception {
        mockMvc.perform(get("/cart/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error/400"));
    }

    @Test
    void returnsNotFoundWithoutExposingInternalException() throws Exception {
        when(cartRepository.findById(999999)).thenReturn(Optional.empty());

        mockMvc.perform(get("/cart/999999"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"))
                .andExpect(handler().handlerType(CartController.class));
    }

    @Test
    void rejectsExcessivePageSize() throws Exception {
        mockMvc.perform(get("/cart").param("size", "10000000"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error/400"));

        verify(cartRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void updateOnlyChangesAllowedFields() throws Exception {
        Cart existing = new Cart();
        existing.setId(7);
        existing.setUserId("old-user");
        existing.setSessionId("old-session");
        LocalDateTime createdAt = LocalDateTime.of(2025, 1, 1, 10, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2025, 1, 2, 10, 0);
        existing.setCreatedAt(createdAt);
        existing.setUpdatedAt(updatedAt);
        when(cartRepository.findById(7)).thenReturn(Optional.of(existing));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/cart/7")
                        .param("userId", "new-user")
                        .param("sessionId", "new-session")
                        .param("id", "12345")
                        .param("createdAt", "2000-01-01T00:00:00")
                        .param("updatedAt", "2000-01-01T00:00:00"))
                .andExpect(status().is3xxRedirection());

        assertEquals(7, existing.getId());
        assertEquals(createdAt, existing.getCreatedAt());
        assertEquals(updatedAt, existing.getUpdatedAt());
        assertEquals("new-user", existing.getUserId());
        assertEquals("new-session", existing.getSessionId());
    }

    @Test
    void deletesThroughPostAndRequiresAnExistingCart() throws Exception {
        Cart existing = new Cart();
        existing.setId(8);
        when(cartRepository.findById(8)).thenReturn(Optional.of(existing));

        mockMvc.perform(post("/cart/8/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/cart"));

        verify(cartRepository).delete(existing);
    }

    @Test
    void hidesUnexpectedPersistenceErrorsBehindGeneric500Page() throws Exception {
        when(cartRepository.findAll(any(Pageable.class)))
                .thenThrow(new IllegalStateException("database details must not reach the client"));

        mockMvc.perform(get("/cart"))
                .andExpect(status().isInternalServerError())
                .andExpect(view().name("error/500"));
    }

    @Test
    void usesBoundedPageQueryForListing() throws Exception {
        when(cartRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(java.util.List.of()));

        mockMvc.perform(get("/cart").param("page", "1").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(view().name("cart/list"));

        verify(cartRepository).findAll(any(Pageable.class));
    }
}
