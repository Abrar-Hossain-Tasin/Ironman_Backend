package com.ironman.service;

import com.ironman.dto.payment.DeliveryEarningsResponse;
import com.ironman.dto.payment.PaymentResponse;
import com.ironman.model.Payment;
import com.ironman.model.User;
import com.ironman.repository.PaymentRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cash-on-hand summary for a delivery agent. Sums every payment row collected
 * by the calling user within the requested window. Defaults to "today" so the
 * delivery dashboard can show a daily counter without sending params.
 */
@Service
@RequiredArgsConstructor
public class DeliveryEarningsService {
  private static final ZoneId DHAKA = ZoneId.of("Asia/Dhaka");

  private final PrincipalService principalService;
  private final PaymentRepository paymentRepository;

  @Transactional(readOnly = true)
  public DeliveryEarningsResponse myEarnings(LocalDate from, LocalDate to) {
    User user = principalService.currentUser();
    LocalDate today = LocalDate.now(DHAKA);
    LocalDate effectiveFrom = from == null ? today : from;
    LocalDate effectiveTo = to == null ? today : to;
    if (effectiveTo.isBefore(effectiveFrom)) {
      LocalDate swap = effectiveTo;
      effectiveTo = effectiveFrom;
      effectiveFrom = swap;
    }

    Instant fromInstant = effectiveFrom.atStartOfDay(DHAKA).toInstant();
    Instant toInstant = effectiveTo.plusDays(1).atStartOfDay(DHAKA).toInstant();

    BigDecimal total = paymentRepository.sumCollectedBy(user.getId(), fromInstant, toInstant);
    long count = paymentRepository.countCollectedBy(user.getId(), fromInstant, toInstant);

    // For the panel itself we also want the underlying rows — but only the
    // ones inside the window. Walking the staff-history list is fine here
    // because a single delivery man rarely has more than a few hundred per day.
    List<Payment> all = paymentRepository.findByCollectedByIdOrderByCollectedAtDesc(user.getId());
    List<PaymentResponse> windowed = all.stream()
        .filter(p -> p.getCollectedAt() != null
            && !p.getCollectedAt().isBefore(fromInstant)
            && p.getCollectedAt().isBefore(toInstant))
        .map(PaymentResponse::from)
        .toList();

    return new DeliveryEarningsResponse(effectiveFrom, effectiveTo, total, count, windowed);
  }
}
