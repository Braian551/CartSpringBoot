package com.example.cart.dto;

import java.time.LocalDateTime;

import com.example.cart.entity.Cart;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Representación pública y estable de un carrito para consumidores HTTP. */
public record CartSummaryResponse(
        Integer id,
        @JsonProperty("user_id") String userId,
        @JsonProperty("session_id") String sessionId,
        @JsonProperty("created_at") LocalDateTime createdAt,
        @JsonProperty("updated_at") LocalDateTime updatedAt) {

    public static CartSummaryResponse from(Cart cart) {
        return new CartSummaryResponse(
                cart.getId(),
                cart.getUserId(),
                cart.getSessionId(),
                cart.getCreatedAt(),
                cart.getUpdatedAt());
    }
}
