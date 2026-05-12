package com.ironman.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record PickupReconcileRequest(
    @Valid @NotEmpty List<ItemCount> items,
    String notes
) {
  public record ItemCount(
      @NotNull UUID itemId,
      @Min(0) int actualQuantity
  ) {}
}
