package com.ironman.service;

import com.ironman.config.BadRequestException;
import com.ironman.config.SlotProperties;
import com.ironman.dto.pricing.SlotAvailabilityResponse;
import com.ironman.dto.pricing.SlotAvailabilityResponse.SlotRow;
import com.ironman.repository.LaundryOrderRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SlotService {
  private static final ZoneId DHAKA = ZoneId.of("Asia/Dhaka");

  private final SlotProperties props;
  private final LaundryOrderRepository orderRepository;

  @Transactional(readOnly = true)
  public SlotAvailabilityResponse availability(LocalDate date) {
    if (date == null) {
      throw new BadRequestException("date is required");
    }
    if (date.isBefore(LocalDate.now(DHAKA))) {
      throw new BadRequestException("date cannot be in the past");
    }
    int cap = Math.max(1, props.getCapacityDefault());

    List<SlotRow> pickupRows = new ArrayList<>();
    for (String slot : props.getPickup()) {
      long booked = orderRepository.countActivePickupsForSlot(date, slot);
      long remaining = Math.max(0L, cap - booked);
      pickupRows.add(new SlotRow(slot, cap, booked, remaining, remaining == 0L));
    }
    List<SlotRow> deliveryRows = new ArrayList<>();
    for (String slot : props.getDelivery()) {
      long booked = orderRepository.countActiveDeliveriesForSlot(date, slot);
      long remaining = Math.max(0L, cap - booked);
      deliveryRows.add(new SlotRow(slot, cap, booked, remaining, remaining == 0L));
    }
    return new SlotAvailabilityResponse(date, pickupRows, deliveryRows);
  }
}
