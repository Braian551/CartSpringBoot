package com.example.cart.controller;

import com.example.cart.config.CartProperties;
import com.example.cart.dto.CartCreateRequest;
import com.example.cart.dto.CartUpdateRequest;
import com.example.cart.entity.Cart;
import com.example.cart.exception.CartNotFoundException;
import com.example.cart.exception.InvalidCartRequestException;
import com.example.cart.repository.CartRepository;

import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/cart")
public class CartController {

    private static final Logger log = LoggerFactory.getLogger(CartController.class);

    private final CartRepository cartRepository;
    private final CartProperties cartProperties;

    public CartController(CartRepository cartRepository, CartProperties cartProperties) {
        this.cartRepository = cartRepository;
        this.cartProperties = cartProperties;
    }

    @InitBinder("cart")
    void configureCartBinding(WebDataBinder binder) {
        binder.setAllowedFields("userId", "sessionId");
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/cart";
    }

    @GetMapping
    public String listCarts(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", required = false) Integer requestedSize,
            @RequestParam(name = "userId", defaultValue = "") String requestedUserId,
            @RequestParam(name = "sessionId", defaultValue = "") String requestedSessionId,
            @RequestParam(name = "orSearch", defaultValue = "") String requestedOrSearch,
            Model model) {
        int size = requestedSize == null ? cartProperties.getDefaultPageSize() : requestedSize;
        validatePageParameters(page, size);
        String userId = normalizeSearch(requestedUserId, 50);
        String sessionId = normalizeSearch(requestedSessionId, 255);
        String orSearch = normalizeSearch(requestedOrSearch, 255);

        if (!orSearch.isBlank() && (!userId.isBlank() || !sessionId.isBlank())) {
            throw new InvalidCartRequestException();
        }

        try {
            PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"));
            Page<Cart> cartsPage = findCarts(pageRequest, userId, sessionId, orSearch);

            model.addAttribute("carts", cartsPage.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("displayPage", page + 1);
            model.addAttribute("pageSize", size);
            model.addAttribute("totalPages", cartsPage.getTotalPages());
            model.addAttribute("hasPrevious", cartsPage.hasPrevious());
            model.addAttribute("hasNext", cartsPage.hasNext());
            model.addAttribute("searchUserId", userId);
            model.addAttribute("searchSessionId", sessionId);
            model.addAttribute("orSearch", orSearch);
            model.addAttribute("pageTitle", "Carritos");
            return "cart/list";
        } catch (RuntimeException exception) {
            log.error("Failed to list carts", exception);
            throw exception;
        }
    }

    private Page<Cart> findCarts(PageRequest pageRequest, String userId, String sessionId, String orSearch) {
        if (!orSearch.isBlank()) {
            Integer id = parseSearchId(orSearch);
            log.info("Searching carts with OR across id, userId and sessionId term={}", orSearch);
            return cartRepository.findByUserIdContainingIgnoreCaseOrSessionIdContainingIgnoreCaseOrId(
                    orSearch, orSearch, id, pageRequest);
        }

        if (!userId.isBlank() && !sessionId.isBlank()) {
            log.info("Searching carts with AND userId={} sessionId={}", userId, sessionId);
            return cartRepository.findByUserIdContainingIgnoreCaseAndSessionIdContainingIgnoreCase(
                    userId, sessionId, pageRequest);
        }
        if (!userId.isBlank()) {
            log.info("Searching carts by userId={}", userId);
            return cartRepository.findByUserIdContainingIgnoreCase(userId, pageRequest);
        }
        if (!sessionId.isBlank()) {
            log.info("Searching carts by sessionId={}", sessionId);
            return cartRepository.findBySessionIdContainingIgnoreCase(sessionId, pageRequest);
        }
        return cartRepository.findAll(pageRequest);
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("cart", new CartCreateRequest());
        model.addAttribute("pageTitle", "Crear carrito");
        return "cart/create";
    }

    @PostMapping
    public String createCart(
            @Valid @ModelAttribute("cart") CartCreateRequest request,
        BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            log.warn("Invalid cart request operation=create errorCount={}", bindingResult.getErrorCount());
            model.addAttribute("pageTitle", "Crear carrito");
            return "cart/create";
        }

        Cart cart = toCart(request);
        log.info("Creating cart userId={}", cart.getUserId());
        try {
            Cart savedCart = cartRepository.save(cart);
            log.info("Cart created successfully cartId={} userId={}", savedCart.getId(), savedCart.getUserId());
            return "redirect:/cart";
        } catch (RuntimeException exception) {
            log.error("Failed to create cart userId={}", cart.getUserId(), exception);
            throw exception;
        }
    }

    @GetMapping("/{id}")
    public String showCart(@PathVariable Integer id, Model model) {
        model.addAttribute("cart", findCart(id));
        model.addAttribute("pageTitle", "Detalle del carrito");
        return "cart/detail";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Integer id, Model model) {
        Cart existingCart = findCart(id);
        model.addAttribute("cart", toUpdateRequest(existingCart));
        model.addAttribute("pageTitle", "Editar carrito");
        return "cart/edit";
    }

    @PostMapping("/{id}")
    public String updateCart(
            @PathVariable Integer id,
            @Valid @ModelAttribute("cart") CartUpdateRequest request,
            BindingResult bindingResult,
            Model model) {
        Cart existingCart = findCart(id);
        if (bindingResult.hasErrors()) {
            log.warn("Invalid cart request operation=update cartId={} errorCount={}", id,
                    bindingResult.getErrorCount());
            model.addAttribute("pageTitle", "Editar carrito");
            return "cart/edit";
        }

        existingCart.setUserId(request.getUserId());
        existingCart.setSessionId(request.getSessionId());
        try {
            Cart updatedCart = cartRepository.save(existingCart);
            log.info("Cart updated cartId={} userId={}", updatedCart.getId(), updatedCart.getUserId());
            return "redirect:/cart/{id}";
        } catch (RuntimeException exception) {
            log.error("Failed to update cart cartId={}", id, exception);
            throw exception;
        }
    }

    @PostMapping("/{id}/delete")
    public String deleteCart(@PathVariable Integer id) {
        Cart existingCart = findCart(id);
        try {
            cartRepository.delete(existingCart);
            log.info("Cart deleted cartId={}", id);
            return "redirect:/cart";
        } catch (RuntimeException exception) {
            log.error("Failed to delete cart cartId={}", id, exception);
            throw exception;
        }
    }

    private Cart findCart(Integer id) {
        validateId(id);
        try {
            return cartRepository.findById(id)
                    .orElseThrow(() -> {
                        log.warn("Cart not found cartId={}", id);
                        return new CartNotFoundException(id);
                    });
        } catch (CartNotFoundException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.error("Failed to find cart cartId={}", id, exception);
            throw exception;
        }
    }

    private Cart toCart(CartCreateRequest request) {
        Cart cart = new Cart();
        cart.setUserId(request.getUserId());
        cart.setSessionId(request.getSessionId());
        return cart;
    }

    private CartUpdateRequest toUpdateRequest(Cart cart) {
        CartUpdateRequest request = new CartUpdateRequest();
        request.setId(cart.getId());
        request.setUserId(cart.getUserId());
        request.setSessionId(cart.getSessionId());
        return request;
    }

    private void validatePageParameters(int page, int size) {
        if (page < 0 || page > cartProperties.getMaxPageNumber()
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
