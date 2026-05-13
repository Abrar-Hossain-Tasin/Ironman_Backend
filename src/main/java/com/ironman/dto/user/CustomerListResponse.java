package com.ironman.dto.user;

import java.util.List;

public record CustomerListResponse(
    List<CustomerSummary> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {}
