package com.ironman.dto.order;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record QuoteResponse(
    List<QuoteLine> lines,
    BigDecimal subtotal,
    BigDecimal discountAmount,
    String appliedCouponCode,
    BigDecimal total
) {
  public record QuoteLine(
      UUID clothingTypeId,
      String clothingTypeName,
      UUID serviceCategoryId,
      String serviceCategoryName,
      int quantity,
      BigDecimal unitPrice,
      BigDecimal subtotal
  ) {}
}
