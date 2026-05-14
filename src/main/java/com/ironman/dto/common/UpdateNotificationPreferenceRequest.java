package com.ironman.dto.common;

import jakarta.validation.constraints.NotBlank;

public record UpdateNotificationPreferenceRequest(
    @NotBlank String channel,
    String notificationType,
    boolean enabled
) {
}
