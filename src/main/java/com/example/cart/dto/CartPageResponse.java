package com.example.cart.dto;

import java.util.List;

/** Respuesta paginada para la consulta administrativa de carritos. */
public record CartPageResponse(
        List<CartSummaryResponse> data,
        Pagination pagination,
        Stats stats) {

    public record Pagination(
            int page,
            int size,
            long totalItems,
            int totalPages) {
    }

    public record Stats(
            long total,
            long authenticated,
            long guests,
            long activeLast30Days) {
    }
}
