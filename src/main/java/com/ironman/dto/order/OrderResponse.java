package com.ironman.dto.order;

import com.ironman.dto.user.AddressResponse;
import com.ironman.dto.user.UserSummary;
import com.ironman.model.CodConfirmationStatus;
import com.ironman.model.LaundryOrder;
import com.ironman.model.OrderStatus;
import com.ironman.model.PaymentMethod;
import com.ironman.model.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
    UUID id,
    String orderNumber,
    UserSummary customer,
    AddressResponse pickupAddress,
    AddressResponse deliveryAddress,
    LocalDate preferredPickupDate,
    String preferredPickupTimeSlot,
    LocalDate preferredDeliveryDate,
    String preferredDeliveryTimeSlot,
    String specialInstructions,
    OrderStatus status,
    PaymentMethod paymentMethod,
    PaymentStatus paymentStatus,
    CodConfirmationStatus codConfirmationStatus,
    Instant customerConfirmedAt,
    Instant deliveryConfirmedAt,
    BigDecimal totalAmount,
    BigDecimal paidAmount,
    List<OrderItemResponse> items,
    Instant createdAt,
    Instant updatedAt
) {
  public static OrderResponse from(LaundryOrder order, List<OrderItemResponse> items) {
    return new OrderResponse(
        order.getId(),
        order.getOrderNumber(),
        UserSummary.from(order.getCustomer()),
        AddressResponse.from(order.getPickupAddress()),
        AddressResponse.from(order.getDeliveryAddress()),
        order.getPreferredPickupDate(),
        order.getPreferredPickupTimeSlot(),
        order.getPreferredDeliveryDate(),
        order.getPreferredDeliveryTimeSlot(),
        order.getSpecialInstructions(),
        order.getStatus(),
        order.getPaymentMethod(),
        order.getPaymentStatus(),
        order.getCodConfirmationStatus(),
        order.getCustomerConfirmedAt(),
        order.getDeliveryConfirmedAt(),
        order.getTotalAmount(),
        order.getPaidAmount(),
        items,
        order.getCreatedAt(),
        order.getUpdatedAt()
    );
  }
}
