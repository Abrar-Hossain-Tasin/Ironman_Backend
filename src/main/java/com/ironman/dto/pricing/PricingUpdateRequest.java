package com.ironman.dto.pricing;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PricingUpdateRequest(
    @NotNull UUID serviceCategoryId,
    @NotNull UUID clothingTypeId,
    @PositiveOrZero BigDecimal price,
    LocalDate effectiveFrom
) {}
