package com.ironman.dto.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FailDeliveryRequest(
    /** Required so the customer and admin can see why the delivery couldn't land. */
    @NotBlank @Size(max = 500) String reason
) {}
