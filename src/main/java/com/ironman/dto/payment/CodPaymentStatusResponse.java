package com.ironman.dto.payment;

import com.ironman.model.CodConfirmationStatus;
import com.ironman.model.LaundryOrder;
import com.ironman.model.PaymentMethod;
import java.time.Instant;
import java.util.UUID;

public record CodPaymentStatusResponse(
    UUID orderId,
    String orderNumber,
    PaymentMethod paymentMethod,
    CodConfirmationStatus codConfirmationStatus,
    Instant customerConfirmedAt,
    Instant deliveryConfirmedAt
) {
  public static CodPaymentStatusResponse from(LaundryOrder order) {
    return new CodPaymentStatusResponse(
        order.getId(),
        order.getOrderNumber(),
        order.getPaymentMethod(),
        order.getCodConfirmationStatus(),
        order.getCustomerConfirmedAt(),
        order.getDeliveryConfirmedAt()
    );
  }
}
