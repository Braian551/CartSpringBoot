package com.example.cart.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import jakarta.servlet.FilterChain;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockFilterChain;

class RateLimitFilterTest {

    @Test
    void returns429WithRetryAfterAndDoesNotBlockAnotherClient() throws Exception {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setGetPerMinute(1);
        RateLimitService service = new RateLimitService(properties);
        RateLimitFilter filter = new RateLimitFilter(service);

        MockHttpServletResponse firstResponse = invoke(filter, "192.0.2.10");
        assertEquals(200, firstResponse.getStatus());

        MockHttpServletResponse rejectedResponse = invoke(filter, "192.0.2.10");
        assertEquals(429, rejectedResponse.getStatus());
        assertTrue(rejectedResponse.getHeader("Retry-After") != null);
        assertEquals(
                "{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded\"}",
                rejectedResponse.getContentAsString(StandardCharsets.UTF_8));

        MockHttpServletResponse otherClientResponse = invoke(filter, "192.0.2.11");
        assertEquals(200, otherClientResponse.getStatus());
    }

    private MockHttpServletResponse invoke(RateLimitFilter filter, String remoteAddress) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/cart");
        request.setRemoteAddr(remoteAddress);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = new MockFilterChain();
        filter.doFilter(request, response, chain);
        return response;
    }
}
