package com.example.cart.security;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.cart.security.RateLimitService.RateLimitDecision;
import com.example.cart.security.RateLimitService.RateLimitType;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final RateLimitService rateLimitService;

    public RateLimitFilter(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        boolean mvcCartRoute = "/cart".equals(uri) || uri.startsWith("/cart/");
        boolean restCartRoute = "/api/admin/carts".equals(uri) || uri.startsWith("/api/admin/carts/");
        return !mvcCartRoute && !restCartRoute;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        RateLimitType type = resolveType(request);
        String clientId = request.getRemoteAddr();
        if (clientId == null || clientId.isBlank()) {
            clientId = "unknown";
        }

        RateLimitDecision decision = rateLimitService.tryAcquire(clientId, type);
        if (!decision.allowed()) {
            log.warn("Rate limit exceeded client={} method={} uri={} type={}",
                    clientId, request.getMethod(), request.getRequestURI(), type);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", Long.toString(decision.retryAfterSeconds()));
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(
                    "{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private RateLimitType resolveType(HttpServletRequest request) {
        String method = request.getMethod();
        if ("GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method)) {
            return RateLimitType.GET;
        }
        if ("DELETE".equalsIgnoreCase(method)
                || ("POST".equalsIgnoreCase(method) && request.getRequestURI().endsWith("/delete"))) {
            return RateLimitType.DELETE;
        }
        return RateLimitType.POST;
    }
}
