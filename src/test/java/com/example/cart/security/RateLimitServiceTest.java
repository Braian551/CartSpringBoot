package com.example.cart.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

import com.example.cart.security.RateLimitService.RateLimitType;

class RateLimitServiceTest {

    @Test
    void limitsOneClientButKeepsAnotherClientIndependent() {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setGetPerMinute(2);
        RateLimitService service = new RateLimitService(properties);

        assertTrue(service.tryAcquire("client-a", RateLimitType.GET).allowed());
        assertTrue(service.tryAcquire("client-a", RateLimitType.GET).allowed());
        RateLimitService.RateLimitDecision rejected = service.tryAcquire("client-a", RateLimitType.GET);
        assertFalse(rejected.allowed());
        assertTrue(rejected.retryAfterSeconds() > 0);
        assertTrue(service.tryAcquire("client-b", RateLimitType.GET).allowed());
    }

    @Test
    void expiresInactiveBucketsAndNeverExceedsConfiguredCapacity() {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setCacheMaxSize(2);
        AtomicLong now = new AtomicLong(1_000_000_000L);
        RateLimitService service = new RateLimitService(properties, now::get);

        service.tryAcquire("client-a", RateLimitType.GET);
        service.tryAcquire("client-b", RateLimitType.GET);
        service.tryAcquire("client-c", RateLimitType.GET);
        assertEquals(2, service.trackedBucketCount());

        now.addAndGet(TimeUnit.MINUTES.toNanos(properties.getExpirationMinutes()));
        service.removeExpiredBuckets();
        assertEquals(0, service.trackedBucketCount());
    }
}
