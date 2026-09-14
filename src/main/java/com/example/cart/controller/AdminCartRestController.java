package com.example.cart.controller;

import java.time.LocalDateTime;

import com.example.cart.config.CartProperties;
import com.example.cart.dto.CartPageResponse;
import com.example.cart.dto.CartSummaryResponse;
import com.example.cart.entity.Cart;
import com.example.cart.exception.CartNotFoundException;
import com.example.cart.exception.InvalidCartRequestException;
import com.example.cart.repository.CartRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API independiente de consulta para el panel admin de Angelow.
 * Solo expone lectura: la lógica de compra pública continúa en su servicio
 * actual y este proyecto se puede retirar sin modificar el frontend público.
 */
@RestController
@RequestMapping("/api/admin/carts")
public class AdminCartRestController {

    private static final Logger log = LoggerFactory.getLogger(AdminCartRestController.class);

    private final CartRepository cartRepository;
    private final CartProperties cartProperties;

    public AdminCartRestController(CartRepository cartRepository, CartProperties cartProperties) {
        this.cartRepository = cartRepository;
        this.cartProperties = cartProperties;
    }

    @GetMapping
    public CartPageResponse list(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", required = false) Integer requestedSize,
            @RequestParam(name = "search", defaultValue = "") String requestedSearch,
            @RequestParam(name = "userId", defaultValue = "") String requestedUserId,
            @RequestParam(name = "sessionId", defaultValue = "") String requestedSessionId) {
        int size = requestedSize == null ? cartProperties.getDefaultPageSize() : requestedSize;
        validatePageParameters(page, size);

        String search = normalizeSearch(requestedSearch, 255);
        String userId = normalizeSearch(requestedUserId, 50);
        String sessionId = normalizeSearch(requestedSessionId, 255);
        if (!search.isBlank() && (!userId.isBlank() || !sessionId.isBlank())) {
            throw new InvalidCartRequestException();
        }

        Sort sort = Sort.by(Sort.Direction.DESC, "updatedAt")
                .and(Sort.by(Sort.Direction.ASC, "id"));
        PageRequest pageRequest = PageRequest.of(page - 1, size, sort);
        Page<Cart> cartsPage;
        if (!search.isBlank()) {
            Integer id = parseSearchId(search);
            log.info("REST cart search with OR across id, userId and sessionId term={}", search);
            cartsPage = cartRepository.findByUserIdContainingIgnoreCaseOrSessionIdContainingIgnoreCaseOrId(
                    search, search, id, pageRequest);
        } else if (!userId.isBlank() && !sessionId.isBlank()) {
            log.info("REST cart search with AND userId={} sessionId={}", userId, sessionId);
            cartsPage = cartRepository.findByUserIdContainingIgnoreCaseAndSessionIdContainingIgnoreCase(
                    userId, sessionId, pageRequest);
        } else if (!userId.isBlank()) {
            cartsPage = cartRepository.findByUserIdContainingIgnoreCase(userId, pageRequest);
        } else if (!sessionId.isBlank()) {
            cartsPage = cartRepository.findBySessionIdContainingIgnoreCase(sessionId, pageRequest);
        } else {
            cartsPage = cartRepository.findAll(pageRequest);
        }

        return new CartPageResponse(
                cartsPage.getContent().stream().map(CartSummaryResponse::from).toList(),
                new CartPageResponse.Pagination(
                        page,
                        size,
                        cartsPage.getTotalElements(),
                        cartsPage.getTotalPages()),
                buildStats());
    }

    @GetMapping("/{id}")
    public CartSummaryResponse findById(@PathVariable Integer id) {
        validateId(id);
        return cartRepository.findById(id)
                .map(CartSummaryResponse::from)
                .orElseThrow(() -> new CartNotFoundException(id));
    }

    private CartPageResponse.Stats buildStats() {
        return new CartPageResponse.Stats(
                cartRepository.count(),
                cartRepository.countByUserIdIsNotNull(),
                cartRepository.countByUserIdIsNull(),
                cartRepository.countByUpdatedAtAfter(LocalDateTime.now().minusDays(30)));
    }

    private void validatePageParameters(int page, int size) {
        if (page < 1 || page - 1 > cartProperties.getMaxPageNumber()
                || size < 1 || size > cartProperties.getMaxPageSize()) {
            throw new InvalidCartRequestException();
        }
    }

    private void validateId(Integer id) {
        if (id == null || id <= 0) {
            throw new InvalidCartRequestException();
        }
    }

    private String normalizeSearch(String value, int maxLength) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.length() > maxLength
                || normalized.chars().anyMatch(Character::isISOControl)) {
            throw new InvalidCartRequestException();
        }
        return normalized;
    }

    private Integer parseSearchId(String search) {
        try {
            return Integer.valueOf(search);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
