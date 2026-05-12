package com.ironman.dto.order;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record ReviewRequest(
    @Min(1) @Max(5) int overallRating,
    @Min(1) @Max(5) Integer serviceRating,
    @Min(1) @Max(5) Integer deliveryRating,
    String comment
) {}
