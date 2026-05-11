package com.ironman.dto.location;

import com.ironman.model.DeliveryLocation;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LocationResponse(
        UUID deliveryManId,
        String deliveryManName,
        BigDecimal latitude,
        BigDecimal longitude,
        BigDecimal accuracy,
        UUID orderId,
        String orderNumber,
        Instant updatedAt
) {
    public static LocationResponse from(DeliveryLocation loc) {
        return new LocationResponse(
                loc.getDeliveryMan().getId(),
                loc.getDeliveryMan().getFullName(),
                loc.getLatitude(),
                loc.getLongitude(),
                loc.getAccuracy(),
                loc.getOrder() != null ? loc.getOrder().getId()          : null,
                loc.getOrder() != null ? loc.getOrder().getOrderNumber() : null,
                loc.getUpdatedAt()
        );
    }
}