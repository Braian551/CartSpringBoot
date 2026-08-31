package com.example.cart.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;

@Component
@Validated
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {

    @Min(1)
    private int getPerMinute = 120;
    @Min(1)
    private int postPerMinute = 30;
    @Min(1)
    private int deletePerMinute = 20;
    @Min(1)
    private int cacheMaxSize = 10_000;
    @Min(1)
    private int expirationMinutes = 10;
    @Min(1_000)
    private long cleanupIntervalMs = 60_000;

    public int getGetPerMinute() {
        return getPerMinute;
    }

    public void setGetPerMinute(int getPerMinute) {
        this.getPerMinute = getPerMinute;
    }

    public int getPostPerMinute() {
        return postPerMinute;
    }

    public void setPostPerMinute(int postPerMinute) {
        this.postPerMinute = postPerMinute;
    }

    public int getDeletePerMinute() {
        return deletePerMinute;
    }

    public void setDeletePerMinute(int deletePerMinute) {
        this.deletePerMinute = deletePerMinute;
    }

    public int getCacheMaxSize() {
        return cacheMaxSize;
    }

    public void setCacheMaxSize(int cacheMaxSize) {
        this.cacheMaxSize = cacheMaxSize;
    }

    public int getExpirationMinutes() {
        return expirationMinutes;
    }

    public void setExpirationMinutes(int expirationMinutes) {
        this.expirationMinutes = expirationMinutes;
    }

    public long getCleanupIntervalMs() {
        return cleanupIntervalMs;
    }

    public void setCleanupIntervalMs(long cleanupIntervalMs) {
        this.cleanupIntervalMs = cleanupIntervalMs;
    }
}
