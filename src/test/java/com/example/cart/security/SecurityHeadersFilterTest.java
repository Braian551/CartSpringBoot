package com.example.cart.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.servlet.FilterChain;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class SecurityHeadersFilterTest {

    @Test
    void addsBasicSecurityHeaders() throws Exception {
        SecurityHeadersFilter filter = new SecurityHeadersFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/cart");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals("nosniff", response.getHeader("X-Content-Type-Options"));
        assertEquals("strict-origin-when-cross-origin", response.getHeader("Referrer-Policy"));
        assertEquals(true, response.getHeader("Content-Security-Policy") != null);
    }
}
