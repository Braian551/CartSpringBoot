package com.example.cart.dto;

import jakarta.validation.constraints.Size;

public class CartCreateRequest {

    @Size(max = 50, message = "El ID de usuario no puede superar 50 caracteres")
    private String userId;

    @Size(max = 255, message = "El ID de sesión no puede superar 255 caracteres")
    private String sessionId;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}
