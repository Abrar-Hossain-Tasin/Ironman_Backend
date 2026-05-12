package com.ironman.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import com.ironman.model.PaymentMethod;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PlaceOrderRequest(
    @NotNull UUID pickupAddressId,
    @NotNull UUID deliveryAddressId,
    @NotNull LocalDate preferredPickupDate,
    @NotNull String preferredPickupTimeSlot,
    @NotNull LocalDate preferredDeliveryDate,
    @NotNull String preferredDeliveryTimeSlot,
    String specialInstructions,
    PaymentMethod paymentMethod,
    String couponCode,
    @Valid @NotEmpty List<OrderItemRequest> items
) {}
