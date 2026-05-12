package com.ironman.dto.order;

import com.ironman.model.OrderTracking;
import com.ironman.model.UserRole;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TrackingResponse(
    UUID id,
    UUID orderId,
    String status,
    String statusLabel,
    String description,
    UUID updatedBy,
    String updatedByName,
    UserRole actorRole,
    BigDecimal locationLat,
    BigDecimal locationLng,
    Instant timestamp
) {
  public static TrackingResponse from(OrderTracking tracking) {
    var user = tracking.getUpdatedBy();
    return new TrackingResponse(
        tracking.getId(),
        tracking.getOrder().getId(),
        tracking.getStatus(),
        tracking.getStatusLabel(),
        tracking.getDescription(),
        user == null ? null : user.getId(),
        user == null ? "System" : user.getFullName(),
        tracking.getActorRole(),
        tracking.getLocationLat(),
        tracking.getLocationLng(),
        tracking.getTimestamp()
    );
  }
}
