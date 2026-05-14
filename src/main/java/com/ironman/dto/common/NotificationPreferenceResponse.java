package com.ironman.dto.common;

import com.ironman.model.NotificationPreference;
import java.time.Instant;
import java.util.UUID;

public record NotificationPreferenceResponse(
    UUID id,
    String channel,
    String notificationType,
    boolean enabled,
    Instant updatedAt
) {
  public static NotificationPreferenceResponse from(NotificationPreference preference) {
    return new NotificationPreferenceResponse(
        preference.getId(),
        preference.getChannel(),
        preference.getNotificationType(),
        preference.isEnabled(),
        preference.getUpdatedAt()
    );
  }
}
