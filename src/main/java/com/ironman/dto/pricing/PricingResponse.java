package com.ironman.dto.pricing;

import com.ironman.model.ServicePricing;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PricingResponse(
    UUID id,
    UUID serviceCategoryId,
    String serviceCategoryName,
    UUID clothingTypeId,
    String clothingTypeName,
    BigDecimal price,
    String currency,
    LocalDate effectiveFrom
) {
  public static PricingResponse from(ServicePricing pricing) {
    return new PricingResponse(
        pricing.getId(),
        pricing.getServiceCategory().getId(),
        pricing.getServiceCategory().getName(),
        pricing.getClothingType().getId(),
        pricing.getClothingType().getName(),
        pricing.getPrice(),
        pricing.getCurrency(),
        pricing.getEffectiveFrom()
    );
  }
}
