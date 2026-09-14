package com.example.cart.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;

import com.example.cart.entity.Cart;

public interface CartRepository extends JpaRepository<Cart, Integer> {

    /** Busca simultáneamente por los dos identificadores (operador AND). */
    Page<Cart> findByUserIdContainingIgnoreCaseAndSessionIdContainingIgnoreCase(
            String userId, String sessionId, Pageable pageable);

    /** Busca por tres campos (id, usuario o sesión) con operador OR. */
    Page<Cart> findByUserIdContainingIgnoreCaseOrSessionIdContainingIgnoreCaseOrId(
            String userId, String sessionId, Integer id, Pageable pageable);

    Page<Cart> findByUserIdContainingIgnoreCase(String userId, Pageable pageable);

    Page<Cart> findBySessionIdContainingIgnoreCase(String sessionId, Pageable pageable);

    Page<Cart> findByUserIdContainingIgnoreCaseOrSessionIdContainingIgnoreCase(
            String userId, String sessionId, Pageable pageable);

    long countByUserIdIsNull();

    long countByUserIdIsNotNull();

    long countByUpdatedAtAfter(LocalDateTime dateTime);

}
