package com.ironman.dto.order;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record OrderItemRequest(
    @NotNull UUID clothingTypeId,
    @NotNull UUID serviceCategoryId,
    @Min(1) int quantity,
    String notes
) {}
