package com.ironman.dto.common;

import com.ironman.model.Notification;
import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
    UUID id,
    String title,
    String body,
    String type,
    UUID referenceId,
    boolean read,
    Instant createdAt
) {
  public static NotificationResponse from(Notification notification) {
    return new NotificationResponse(
        notification.getId(),
        notification.getTitle(),
        notification.getBody(),
        notification.getType(),
        notification.getReferenceId(),
        notification.isRead(),
        notification.getCreatedAt()
    );
  }
}
