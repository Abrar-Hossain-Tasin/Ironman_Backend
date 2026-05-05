package com.ironman.dto.order;

import com.ironman.model.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record StatusUpdateRequest(
    @NotNull OrderStatus status,
    String reason
) {}
