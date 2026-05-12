package com.ironman.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record QuoteRequest(
    @Valid @NotEmpty List<OrderItemRequest> items,
    String couponCode
) {}
