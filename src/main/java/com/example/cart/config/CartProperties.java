package com.example.cart.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

@Component
@Validated
@ConfigurationProperties(prefix = "app.cart")
public class CartProperties {

    @Min(1)
    @Max(100)
    private int defaultPageSize = 20;
    @Min(1)
    @Max(100)
    private int maxPageSize = 100;
    @Min(0)
    private int maxPageNumber = 10_000;

    public int getDefaultPageSize() {
        return defaultPageSize;
    }

    public void setDefaultPageSize(int defaultPageSize) {
        this.defaultPageSize = defaultPageSize;
    }

    public int getMaxPageSize() {
        return maxPageSize;
    }

    public void setMaxPageSize(int maxPageSize) {
        this.maxPageSize = maxPageSize;
    }

    public int getMaxPageNumber() {
        return maxPageNumber;
    }

    public void setMaxPageNumber(int maxPageNumber) {
        this.maxPageNumber = maxPageNumber;
    }
}
