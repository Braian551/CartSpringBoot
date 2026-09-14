package com.example.cart.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "carts")
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id", length = 50)
    @Size(max = 50, message = "El ID de usuario no puede superar 50 caracteres")
    @Pattern(regexp = "^[^\\p{Cntrl}]*$", message = "El ID de usuario no puede contener caracteres de control")
    private String userId;

    @Column(name = "session_id", length = 255)
    @Size(max = 255, message = "El ID de sesión no puede superar 255 caracteres")
    @Pattern(regexp = "^[^\\p{Cntrl}]*$", message = "El ID de sesión no puede contener caracteres de control")
    private String sessionId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Cart() {
    }

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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * La base establece ambas fechas automáticamente. La validación queda en
     * la entidad para proteger también los usos que no pasan por el MVC.
     * Los null son válidos antes de que se ejecuten los callbacks de JPA.
     */
    @AssertTrue(message = "Las fechas del carrito no pueden estar en el futuro")
    public boolean hasValidDates() {
        LocalDateTime now = LocalDateTime.now();
        return (createdAt == null || !createdAt.isAfter(now))
                && (updatedAt == null || !updatedAt.isAfter(now));
    }

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
