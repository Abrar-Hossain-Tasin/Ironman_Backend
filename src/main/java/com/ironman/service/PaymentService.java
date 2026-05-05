package com.ironman.service;

import com.ironman.config.NotFoundException;
import com.ironman.dto.payment.PaymentRecordRequest;
import com.ironman.dto.payment.PaymentResponse;
import com.ironman.model.LaundryOrder;
import com.ironman.model.Payment;
import com.ironman.model.PaymentStatus;
import com.ironman.model.User;
import com.ironman.repository.LaundryOrderRepository;
import com.ironman.repository.PaymentRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentService {
  private final PrincipalService principalService;
  private final PaymentRepository paymentRepository;
  private final LaundryOrderRepository orderRepository;

  @Transactional
  public PaymentResponse record(PaymentRecordRequest request) {
    User collector = principalService.currentUser();
    LaundryOrder order = orderRepository.findById(request.orderId())
        .orElseThrow(() -> new NotFoundException("Order not found"));

    var payment = new Payment();
    payment.setOrder(order);
    payment.setCollectedBy(collector);
    payment.setAmount(request.amount());
    payment.setPaymentType(request.paymentType());
    payment.setNotes(request.notes());
    payment = paymentRepository.save(payment);

    var paid = order.getPaidAmount().add(request.amount());
    order.setPaidAmount(paid);
    order.setPaymentStatus(paid.compareTo(order.getTotalAmount()) >= 0 ? PaymentStatus.paid : PaymentStatus.partial);
    orderRepository.save(order);

    return PaymentResponse.from(payment);
  }

  public List<PaymentResponse> forOrder(UUID orderId) {
    return paymentRepository.findByOrderIdOrderByCollectedAtDesc(orderId).stream()
        .map(PaymentResponse::from)
        .toList();
  }

  public List<PaymentResponse> forStaff(UUID staffId) {
    return paymentRepository.findByCollectedByIdOrderByCollectedAtDesc(staffId).stream()
        .map(PaymentResponse::from)
        .toList();
  }

  public List<PaymentResponse> ledger() {
    return paymentRepository.findAllByOrderByCollectedAtDesc().stream()
        .map(PaymentResponse::from)
        .toList();
  }

  @Transactional
  public PaymentResponse verify(UUID id) {
    User admin = principalService.currentUser();
    Payment payment = paymentRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Payment not found"));
    payment.setVerified(true);
    payment.setVerifiedBy(admin);
    payment.setVerifiedAt(Instant.now());
    return PaymentResponse.from(paymentRepository.save(payment));
  }
}
