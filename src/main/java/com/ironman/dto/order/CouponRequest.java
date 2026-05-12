package com.ironman.dto.order;

import com.ironman.model.DiscountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;

public record CouponRequest(
    @NotBlank String code,
    String description,
    @NotNull DiscountType discountType,
    @NotNull @Positive BigDecimal discountValue,
    BigDecimal minOrderAmount,
    BigDecimal maxDiscountAmount,
    Instant validFrom,
    Instant validTo,
    Integer maxUses,
    Integer maxUsesPerUser,
    Boolean active
) {}
