package com.example.cart.security;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityHeadersFilter extends OncePerRequestFilter {

    private static final String CONTENT_SECURITY_POLICY =
            "default-src 'self'; "
                    + "script-src 'self' 'unsafe-inline' https://cdn.tailwindcss.com; "
                    + "style-src 'self' 'unsafe-inline'; "
                    + "connect-src 'self' https://cdn.tailwindcss.com; "
                    + "img-src 'self' data:; "
                    + "base-uri 'self'; form-action 'self'; frame-ancestors 'none'";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        response.setHeader("Content-Security-Policy", CONTENT_SECURITY_POLICY);
        filterChain.doFilter(request, response);
    }
}
