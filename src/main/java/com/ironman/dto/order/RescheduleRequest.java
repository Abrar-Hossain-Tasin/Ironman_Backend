package com.ironman.dto.order;

import java.time.LocalDate;

public record RescheduleRequest(
    LocalDate preferredPickupDate,
    String preferredPickupTimeSlot,
    LocalDate preferredDeliveryDate,
    String preferredDeliveryTimeSlot,
    String reason
) {}
