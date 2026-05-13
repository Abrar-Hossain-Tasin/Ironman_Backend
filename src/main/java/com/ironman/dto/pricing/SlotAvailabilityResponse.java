package com.ironman.dto.pricing;

import java.time.LocalDate;
import java.util.List;

/**
 * Capacity envelope for a single day. Each slot reports its current load and
 * remaining capacity so the wizard can grey out anything full and surface
 * "limited" hints when remaining is low.
 */
public record SlotAvailabilityResponse(
    LocalDate date,
    List<SlotRow> pickup,
    List<SlotRow> delivery
) {
  public record SlotRow(
      String slot,
      int capacity,
      long booked,
      long remaining,
      boolean full
  ) {}
}
