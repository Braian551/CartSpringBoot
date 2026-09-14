package com.example.cart.dto;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public class CartUpdateRequest {

    private Integer id;

    @Size(max = 50, message = "El ID de usuario no puede superar 50 caracteres")
    @Pattern(regexp = "^[^\\p{Cntrl}]*$", message = "El ID de usuario no puede contener caracteres de control")
    private String userId;

    @Size(max = 255, message = "El ID de sesión no puede superar 255 caracteres")
    @Pattern(regexp = "^[^\\p{Cntrl}]*$", message = "El ID de sesión no puede contener caracteres de control")
    private String sessionId;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

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
