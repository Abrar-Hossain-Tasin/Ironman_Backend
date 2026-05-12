package com.ironman.dto.order;

import java.util.List;

public record OrderSearchResponse(
    List<OrderResponse> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {}
