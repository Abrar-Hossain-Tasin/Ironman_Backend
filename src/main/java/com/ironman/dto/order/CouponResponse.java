package com.ironman.dto.order;

import com.ironman.model.Coupon;
import com.ironman.model.DiscountType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CouponResponse(
    UUID id,
    String code,
    String description,
    DiscountType discountType,
    BigDecimal discountValue,
    BigDecimal minOrderAmount,
    BigDecimal maxDiscountAmount,
    Instant validFrom,
    Instant validTo,
    Integer maxUses,
    int currentUses,
    int maxUsesPerUser,
    boolean active
) {
  public static CouponResponse from(Coupon c) {
    return new CouponResponse(
        c.getId(), c.getCode(), c.getDescription(),
        c.getDiscountType(), c.getDiscountValue(), c.getMinOrderAmount(),
        c.getMaxDiscountAmount(), c.getValidFrom(), c.getValidTo(),
        c.getMaxUses(), c.getCurrentUses(), c.getMaxUsesPerUser(), c.isActive());
  }
}
