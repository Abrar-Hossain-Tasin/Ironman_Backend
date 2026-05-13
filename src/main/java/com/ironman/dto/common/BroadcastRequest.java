package com.ironman.dto.common;

import com.ironman.model.UserRole;
import jakarta.validation.constraints.NotBlank;

public record BroadcastRequest(
    @NotBlank String title,
    @NotBlank String body,
    UserRole role
) {}
