package com.example.cart.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.example.cart.config.CartProperties;
import com.example.cart.controller.CartController;
import com.example.cart.exception.GlobalExceptionHandler;
import com.example.cart.repository.CartRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.example.cart.security.RateLimitService.RateLimitDecision;
import com.example.cart.security.RateLimitService.RateLimitType;

@WebMvcTest(CartController.class)
@AutoConfigureMockMvc
@ImportAutoConfiguration(ServletWebSecurityAutoConfiguration.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, CartProperties.class})
class CsrfProtectionTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CartRepository cartRepository;

    @MockitoBean
    private RateLimitService rateLimitService;

    @BeforeEach
    void allowRateLimitedRequestsInThisSlice() {
        when(rateLimitService.tryAcquire(anyString(), any(RateLimitType.class)))
                .thenReturn(new RateLimitDecision(true, 0));
    }

    @Test
    void rejectsStateChangingRequestWithoutCsrfToken() throws Exception {
        mockMvc.perform(post("/cart").param("userId", "user-1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void exposesCsrfTokenToThymeleafForms() throws Exception {
        mockMvc.perform(get("/cart/new"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")));
    }
}
