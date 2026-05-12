package com.ironman.dto.order;

import com.ironman.model.OrderItem;
import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(
    UUID id,
    UUID clothingTypeId,
    String clothingTypeName,
    UUID serviceCategoryId,
    String serviceCategoryName,
    int quantity,
    Integer actualQuantity,
    BigDecimal unitPrice,
    BigDecimal subtotal,
    String notes
) {
  public static OrderItemResponse from(OrderItem item) {
    return new OrderItemResponse(
        item.getId(),
        item.getClothingType().getId(),
        item.getClothingType().getName(),
        item.getServiceCategory().getId(),
        item.getServiceCategory().getName(),
        item.getQuantity(),
        item.getActualQuantity(),
        item.getUnitPrice(),
        item.getSubtotal(),
        item.getNotes()
    );
  }
}
