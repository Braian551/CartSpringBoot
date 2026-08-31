package com.example.cart.security;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.LongSupplier;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class RateLimitService {

    private static final long WINDOW_NANOS = TimeUnit.MINUTES.toNanos(1);

    private final RateLimitProperties properties;
    private final ReentrantLock lock = new ReentrantLock();
    private final Map<String, ClientBucket> buckets = new LinkedHashMap<>(16, 0.75f, true);
    private final long expirationNanos;
    private final LongSupplier nanoTime;

    @Autowired
    public RateLimitService(RateLimitProperties properties) {
        this(properties, System::nanoTime);
    }

    RateLimitService(RateLimitProperties properties, LongSupplier nanoTime) {
        this.properties = properties;
        this.expirationNanos = TimeUnit.MINUTES.toNanos(properties.getExpirationMinutes());
        this.nanoTime = nanoTime;
    }

    public RateLimitDecision tryAcquire(String clientId, RateLimitType type) {
        long now = nanoTime.getAsLong();
        int limit = limitFor(type);
        String key = clientId + "|" + type.name();

        lock.lock();
        try {
            removeExpired(now);

            ClientBucket bucket = buckets.get(key);
            if (bucket == null) {
                evictOldestIfFull();
                bucket = new ClientBucket(now);
                buckets.put(key, bucket);
            }

            bucket.lastAccessNanos = now;
            if (now - bucket.windowStartNanos >= WINDOW_NANOS) {
                bucket.windowStartNanos = now;
                bucket.requests = 0;
            }

            if (bucket.requests >= limit) {
                long remainingNanos = WINDOW_NANOS - (now - bucket.windowStartNanos);
                long retryAfter = Math.max(1,
                        TimeUnit.NANOSECONDS.toSeconds(remainingNanos + TimeUnit.SECONDS.toNanos(1) - 1));
                return new RateLimitDecision(false, retryAfter);
            }

            bucket.requests++;
            return new RateLimitDecision(true, 0);
        } finally {
            lock.unlock();
        }
    }

    @Scheduled(fixedDelayString = "${app.rate-limit.cleanup-interval-ms:60000}")
    public void removeExpiredBuckets() {
        lock.lock();
        try {
            removeExpired(nanoTime.getAsLong());
        } finally {
            lock.unlock();
        }
    }

    int trackedBucketCount() {
        lock.lock();
        try {
            return buckets.size();
        } finally {
            lock.unlock();
        }
    }

    private int limitFor(RateLimitType type) {
        return switch (type) {
            case GET -> properties.getGetPerMinute();
            case POST -> properties.getPostPerMinute();
            case DELETE -> properties.getDeletePerMinute();
        };
    }

    private void removeExpired(long now) {
        Iterator<Map.Entry<String, ClientBucket>> iterator = buckets.entrySet().iterator();
        while (iterator.hasNext()) {
            ClientBucket bucket = iterator.next().getValue();
            if (now - bucket.lastAccessNanos >= expirationNanos) {
                iterator.remove();
            }
        }
    }

    private void evictOldestIfFull() {
        while (buckets.size() >= properties.getCacheMaxSize()) {
            Iterator<String> iterator = buckets.keySet().iterator();
            if (!iterator.hasNext()) {
                return;
            }
            iterator.next();
            iterator.remove();
        }
    }

    public enum RateLimitType {
        GET,
        POST,
        DELETE
    }

    public record RateLimitDecision(boolean allowed, long retryAfterSeconds) {
    }

    private static final class ClientBucket {

        private long windowStartNanos;
        private long lastAccessNanos;
        private int requests;

        private ClientBucket(long now) {
            this.windowStartNanos = now;
            this.lastAccessNanos = now;
        }
    }
}
