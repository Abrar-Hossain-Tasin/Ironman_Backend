package com.ironman.dto.location;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

public record LocationUpdateRequest(
        @NotNull @DecimalMin("-90.0")  @DecimalMax("90.0")  BigDecimal latitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude,
        BigDecimal accuracy,   // optional, metres
        UUID orderId           // optional, current active order
) {}