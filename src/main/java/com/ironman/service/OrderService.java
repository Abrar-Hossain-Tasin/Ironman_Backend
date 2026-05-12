package com.ironman.service;

import com.ironman.config.BadRequestException;
import com.ironman.config.NotFoundException;
import com.ironman.dto.order.OrderItemResponse;
import com.ironman.dto.order.OrderItemRequest;
import com.ironman.dto.order.OrderResponse;
import com.ironman.dto.order.PlaceOrderRequest;
import com.ironman.dto.order.QuoteRequest;
import com.ironman.dto.order.QuoteResponse;
import com.ironman.dto.order.RescheduleRequest;
import com.ironman.dto.order.TrackingResponse;
import com.ironman.model.CodConfirmationStatus;
import com.ironman.model.LaundryOrder;
import com.ironman.model.OrderItem;
import com.ironman.model.OrderStatus;
import com.ironman.model.OrderTracking;
import com.ironman.model.PaymentStatus;
import com.ironman.model.User;
import com.ironman.model.UserRole;
import com.ironman.repository.AddressRepository;
import com.ironman.repository.ClothingTypeRepository;
import com.ironman.repository.CustomerProfileRepository;
import com.ironman.repository.LaundryOrderRepository;
import com.ironman.repository.OrderItemRepository;
import com.ironman.repository.OrderTrackingRepository;
import com.ironman.repository.ServiceCategoryRepository;
import com.ironman.repository.ServicePricingRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {

  private static final ZoneId DHAKA = ZoneId.of("Asia/Dhaka");
  private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = Map.ofEntries(
          Map.entry(OrderStatus.pending,
                  Set.of(OrderStatus.confirmed, OrderStatus.cancelled)),
          Map.entry(OrderStatus.confirmed,
                  Set.of(OrderStatus.pickup_assigned, OrderStatus.cancelled)),
          Map.entry(OrderStatus.pickup_assigned,
                  Set.of(OrderStatus.picked_up, OrderStatus.cancelled)),
          Map.entry(OrderStatus.picked_up,
                  Set.of(OrderStatus.in_wash, OrderStatus.in_dry_clean,
                          OrderStatus.waiting_for_iron, OrderStatus.in_iron,
                          OrderStatus.ready, OrderStatus.cancelled)),
          Map.entry(OrderStatus.in_wash,
                  Set.of(OrderStatus.wash_complete, OrderStatus.cancelled)),
          Map.entry(OrderStatus.wash_complete,
                  Set.of(OrderStatus.waiting_for_iron, OrderStatus.in_iron,
                          OrderStatus.ready, OrderStatus.cancelled)),
          Map.entry(OrderStatus.in_dry_clean,
                  Set.of(OrderStatus.dry_clean_complete, OrderStatus.cancelled)),
          Map.entry(OrderStatus.dry_clean_complete,
                  Set.of(OrderStatus.waiting_for_iron, OrderStatus.in_iron,
                          OrderStatus.ready, OrderStatus.cancelled)),
          Map.entry(OrderStatus.waiting_for_iron,
                  Set.of(OrderStatus.in_iron, OrderStatus.cancelled)),
          Map.entry(OrderStatus.in_iron,
                  Set.of(OrderStatus.iron_complete, OrderStatus.cancelled)),
          Map.entry(OrderStatus.iron_complete,
                  Set.of(OrderStatus.ready, OrderStatus.cancelled)),
          Map.entry(OrderStatus.ready,
                  Set.of(OrderStatus.delivery_assigned, OrderStatus.cancelled)),
          Map.entry(OrderStatus.delivery_assigned,
                  Set.of(OrderStatus.out_for_delivery, OrderStatus.cancelled)),
          Map.entry(OrderStatus.out_for_delivery,
                  Set.of(OrderStatus.delivered, OrderStatus.delivery_failed,
                          OrderStatus.cancelled)),
          Map.entry(OrderStatus.delivered,
                  Set.of(OrderStatus.disputed, OrderStatus.returned)),
          Map.entry(OrderStatus.delivery_failed,
                  Set.of(OrderStatus.delivery_assigned, OrderStatus.returned,
                          OrderStatus.cancelled)),
          Map.entry(OrderStatus.disputed,
                  Set.of(OrderStatus.delivered, OrderStatus.returned)),
          Map.entry(OrderStatus.returned, Set.of()),
          Map.entry(OrderStatus.cancelled, Set.of())
  );

  private final PrincipalService principalService;
  private final LaundryOrderRepository orderRepository;
  private final OrderItemRepository orderItemRepository;
  private final OrderTrackingRepository trackingRepository;
  private final AddressRepository addressRepository;
  private final ServicePricingRepository servicePricingRepository;
  private final ClothingTypeRepository clothingTypeRepository;
  private final ServiceCategoryRepository serviceCategoryRepository;
  private final CustomerProfileRepository customerProfileRepository;
  private final NotificationService notificationService;
  private final EmailService emailService;
  private final CouponService couponService;

  @Transactional
  public OrderResponse placeOrder(PlaceOrderRequest request) {
    User customer = principalService.currentUser();
    if (customer.getRole() != UserRole.customer) {
      throw new BadRequestException("Only customers can place orders");
    }

    var pickupAddress = addressRepository
            .findByIdAndUserId(request.pickupAddressId(), customer.getId())
            .orElseThrow(() -> new NotFoundException("Pickup address not found"));
    var deliveryAddress = addressRepository
            .findByIdAndUserId(request.deliveryAddressId(), customer.getId())
            .orElseThrow(() -> new NotFoundException("Delivery address not found"));

    var order = new LaundryOrder();
    order.setCustomer(customer);
    order.setPickupAddress(pickupAddress);
    order.setDeliveryAddress(deliveryAddress);
    order.setPreferredPickupDate(request.preferredPickupDate());
    order.setPreferredPickupTimeSlot(request.preferredPickupTimeSlot());
    order.setPreferredDeliveryDate(request.preferredDeliveryDate());
    order.setPreferredDeliveryTimeSlot(request.preferredDeliveryTimeSlot());
    order.setSpecialInstructions(request.specialInstructions());
    if (request.paymentMethod() != null) {
      order.setPaymentMethod(request.paymentMethod());
    }
    order.setOrderNumber(nextOrderNumber());
    order.setStatus(OrderStatus.pending);
    order.setPaymentStatus(PaymentStatus.pending);
    order = orderRepository.save(order);

    BigDecimal total = BigDecimal.ZERO;
    for (var requestItem : request.items()) {
      var clothingType = clothingTypeRepository.findById(requestItem.clothingTypeId())
              .orElseThrow(() -> new NotFoundException("Clothing type not found"));
      var category = serviceCategoryRepository.findById(requestItem.serviceCategoryId())
              .orElseThrow(() -> new NotFoundException("Service category not found"));
      var pricing = servicePricingRepository
              .findByServiceCategoryIdAndClothingTypeIdAndCurrentTrue(
                      category.getId(), clothingType.getId())
              .orElseThrow(() -> new BadRequestException(
                      "Pricing is unavailable for " + category.getName()
                              + " / " + clothingType.getName()));

      var subtotal = pricing.getPrice().multiply(BigDecimal.valueOf(requestItem.quantity()));
      var item = new OrderItem();
      item.setOrder(order);
      item.setClothingType(clothingType);
      item.setServiceCategory(category);
      item.setQuantity(requestItem.quantity());
      item.setUnitPrice(pricing.getPrice());
      item.setSubtotal(subtotal);
      item.setNotes(requestItem.notes());
      orderItemRepository.save(item);
      total = total.add(subtotal);
    }

    CouponService.AppliedCoupon applied = couponService.validate(
        request.couponCode(), total, customer);
    BigDecimal discount = applied == null ? BigDecimal.ZERO : applied.discountAmount();
    BigDecimal finalTotal = total.subtract(discount).max(BigDecimal.ZERO);

    order.setDiscountAmount(discount);
    if (applied != null) {
      order.setCouponCode(applied.coupon().getCode());
    }
    order.setTotalAmount(finalTotal);
    order = orderRepository.save(order);
    couponService.recordRedemption(applied, order, customer);
    addTracking(order, OrderStatus.pending, "Order placed", customer);

    customerProfileRepository.findByUserId(customer.getId()).ifPresent(profile -> {
      profile.setTotalOrders(profile.getTotalOrders() + 1);
      customerProfileRepository.save(profile);
    });

    // In-app + email notification to admins
    final LaundryOrder savedOrder = order;
    notificationService.notifyAdmins(
            "New order " + order.getOrderNumber(),
            customer.getFullName() + " placed a BDT " + order.getTotalAmount() + " order.",
            "order_created",
            order.getId()
    );
    // Send a richer admin alert email separately
    final String orderNum = order.getOrderNumber();
    final String amount = order.getTotalAmount().toPlainString();
    notificationService.notifyAdmins(
            "[ADMIN] New Order — " + orderNum,
            customer.getFullName() + " placed a BDT " + amount + " order. Please confirm.",
            "order_created_admin",
            order.getId()
    );

    // Confirmation email to the customer
    emailService.sendOrderPlaced(
            customer.getEmail(),
            customer.getFullName(),
            order.getOrderNumber(),
            order.getTotalAmount().toPlainString()
    );

    return toResponse(order);
  }

  @Transactional(readOnly = true)
  public QuoteResponse quote(QuoteRequest request) {
    User customer = principalService.currentUser();
    BigDecimal subtotal = BigDecimal.ZERO;
    var lines = new java.util.ArrayList<QuoteResponse.QuoteLine>();
    for (OrderItemRequest requestItem : request.items()) {
      var clothingType = clothingTypeRepository.findById(requestItem.clothingTypeId())
              .orElseThrow(() -> new NotFoundException("Clothing type not found"));
      var category = serviceCategoryRepository.findById(requestItem.serviceCategoryId())
              .orElseThrow(() -> new NotFoundException("Service category not found"));
      var pricing = servicePricingRepository
              .findByServiceCategoryIdAndClothingTypeIdAndCurrentTrue(
                      category.getId(), clothingType.getId())
              .orElseThrow(() -> new BadRequestException(
                      "Pricing is unavailable for " + category.getName()
                              + " / " + clothingType.getName()));
      BigDecimal lineSub = pricing.getPrice()
          .multiply(BigDecimal.valueOf(requestItem.quantity()));
      lines.add(new QuoteResponse.QuoteLine(
          clothingType.getId(), clothingType.getName(),
          category.getId(), category.getName(),
          requestItem.quantity(), pricing.getPrice(), lineSub));
      subtotal = subtotal.add(lineSub);
    }
    CouponService.AppliedCoupon applied = couponService.validate(
        request.couponCode(), subtotal, customer);
    BigDecimal discount = applied == null ? BigDecimal.ZERO : applied.discountAmount();
    String code = applied == null ? null : applied.coupon().getCode();
    return new QuoteResponse(lines, subtotal, discount, code,
        subtotal.subtract(discount).max(BigDecimal.ZERO));
  }

  @Transactional(readOnly = true)
  public List<OrderResponse> listMineOrAll() {
    User user = principalService.currentUser();
    List<LaundryOrder> orders = user.getRole() == UserRole.admin
            ? orderRepository.findAllByOrderByCreatedAtDesc()
            : orderRepository.findByCustomerIdOrderByCreatedAtDesc(user.getId());
    return orders.stream().map(this::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public List<OrderResponse> listAdmin(OrderStatus status) {
    List<LaundryOrder> orders = status == null
            ? orderRepository.findAllByOrderByCreatedAtDesc()
            : orderRepository.findByStatusOrderByCreatedAtDesc(status);
    return orders.stream().map(this::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public com.ironman.dto.order.OrderSearchResponse search(
      OrderStatus status, java.time.LocalDate from, java.time.LocalDate to,
      java.math.BigDecimal minAmount, java.math.BigDecimal maxAmount,
      int page, int size
  ) {
    User user = principalService.currentUser();
    org.springframework.data.jpa.domain.Specification<LaundryOrder> spec = (root, q, cb) -> {
      var preds = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
      if (user.getRole() == UserRole.customer) {
        preds.add(cb.equal(root.get("customer").get("id"), user.getId()));
      }
      if (status != null) {
        preds.add(cb.equal(root.get("status"), status));
      }
      if (from != null) {
        preds.add(cb.greaterThanOrEqualTo(root.get("createdAt"),
            from.atStartOfDay(DHAKA).toInstant()));
      }
      if (to != null) {
        preds.add(cb.lessThan(root.get("createdAt"),
            to.plusDays(1).atStartOfDay(DHAKA).toInstant()));
      }
      if (minAmount != null) {
        preds.add(cb.greaterThanOrEqualTo(root.get("totalAmount"), minAmount));
      }
      if (maxAmount != null) {
        preds.add(cb.lessThanOrEqualTo(root.get("totalAmount"), maxAmount));
      }
      return cb.and(preds.toArray(new jakarta.persistence.criteria.Predicate[0]));
    };
    var pageable = org.springframework.data.domain.PageRequest.of(
        Math.max(0, page), Math.min(100, Math.max(1, size)),
        org.springframework.data.domain.Sort.by("createdAt").descending());
    var p = orderRepository.findAll(spec, pageable);
    return new com.ironman.dto.order.OrderSearchResponse(
        p.getContent().stream().map(this::toResponse).toList(),
        p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
  }

  @Transactional(readOnly = true)
  public List<OrderResponse> listAdminByCodConfirmation(CodConfirmationStatus codStatus) {
    return orderRepository.findByCodConfirmationStatusOrderByCreatedAtDesc(codStatus)
            .stream().map(this::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public OrderResponse detail(UUID id) {
    return toResponse(scopedOrder(id));
  }

  @Transactional(readOnly = true)
  public List<OrderItemResponse> items(UUID id) {
    LaundryOrder order = scopedOrder(id);
    return orderItemRepository.findByOrderId(order.getId()).stream()
            .map(OrderItemResponse::from)
            .toList();
  }

  @Transactional(readOnly = true)
  public List<TrackingResponse> tracking(UUID id) {
    LaundryOrder order = scopedOrder(id);
    return trackingRepository.findByOrderIdOrderByTimestampAsc(order.getId()).stream()
            .map(TrackingResponse::from)
            .toList();
  }

  @Transactional(readOnly = true)
  public List<TrackingResponse> publicTracking(String orderNumber) {
    LaundryOrder order = orderRepository.findByOrderNumber(orderNumber)
            .orElseThrow(() -> new NotFoundException("Order not found"));
    return trackingRepository.findByOrderIdOrderByTimestampAsc(order.getId()).stream()
            .map(TrackingResponse::from)
            .toList();
  }

  @Transactional
  public OrderResponse reschedule(UUID id, RescheduleRequest request) {
    User actor = principalService.currentUser();
    LaundryOrder order = scopedOrder(id);

    boolean changingPickup = request.preferredPickupDate() != null
        || request.preferredPickupTimeSlot() != null;
    boolean changingDelivery = request.preferredDeliveryDate() != null
        || request.preferredDeliveryTimeSlot() != null;
    if (!changingPickup && !changingDelivery) {
      throw new BadRequestException("Provide a pickup or delivery slot to reschedule");
    }

    if (changingPickup) {
      if (!Set.of(OrderStatus.pending, OrderStatus.confirmed, OrderStatus.pickup_assigned)
          .contains(order.getStatus())) {
        throw new BadRequestException("Pickup cannot be rescheduled after collection");
      }
      if (request.preferredPickupDate() != null) {
        if (request.preferredPickupDate().isBefore(LocalDate.now(DHAKA))) {
          throw new BadRequestException("Pickup date cannot be in the past");
        }
        order.setPreferredPickupDate(request.preferredPickupDate());
      }
      if (request.preferredPickupTimeSlot() != null) {
        order.setPreferredPickupTimeSlot(request.preferredPickupTimeSlot());
      }
    }

    if (changingDelivery) {
      if (Set.of(OrderStatus.delivered, OrderStatus.cancelled, OrderStatus.returned)
          .contains(order.getStatus())) {
        throw new BadRequestException("Delivery cannot be rescheduled for closed orders");
      }
      if (request.preferredDeliveryDate() != null) {
        if (request.preferredDeliveryDate().isBefore(LocalDate.now(DHAKA))) {
          throw new BadRequestException("Delivery date cannot be in the past");
        }
        order.setPreferredDeliveryDate(request.preferredDeliveryDate());
      }
      if (request.preferredDeliveryTimeSlot() != null) {
        order.setPreferredDeliveryTimeSlot(request.preferredDeliveryTimeSlot());
      }
    }

    order.setUpdatedAt(Instant.now());
    order = orderRepository.save(order);

    String note = (request.reason() == null || request.reason().isBlank())
        ? "Schedule updated"
        : "Schedule updated: " + request.reason();
    addTracking(order, order.getStatus(), note, actor);

    notificationService.notifyUser(order.getCustomer(),
        "Schedule updated — " + order.getOrderNumber(),
        "Your order schedule has been updated. " + note,
        "order_rescheduled", order.getId());

    return toResponse(order);
  }

  @Transactional
  public OrderResponse cancel(UUID id) {
    LaundryOrder order = scopedOrder(id);
    if (!Set.of(OrderStatus.pending, OrderStatus.confirmed, OrderStatus.pickup_assigned)
            .contains(order.getStatus())) {
      throw new BadRequestException("Order cannot be cancelled after pickup");
    }
    return updateStatus(order, OrderStatus.cancelled, "Order cancelled",
            principalService.currentUser());
  }

  @Transactional
  public OrderResponse updateStatus(UUID id, OrderStatus next, String reason) {
    return updateStatus(scopedOrder(id), next, reason, principalService.currentUser());
  }

  @Transactional
  public OrderResponse updateStatus(LaundryOrder order, OrderStatus next, String reason,
                                    User actor) {
    if (order.getStatus() == next) {
      return toResponse(order);
    }
    validateTransition(order.getStatus(), next);
    order.setStatus(next);
    order = orderRepository.save(order);
    addTracking(order, next,
            reason == null || reason.isBlank() ? statusLabel(next) : reason, actor);

    User customer = order.getCustomer();
    String notifBody = customerNotification(next, order.getOrderNumber());

    // In-app notification
    notificationService.notifyUser(
            customer, statusLabel(next), notifBody, "order_status", order.getId());

    // Targeted email for key lifecycle events
    sendLifecycleEmail(customer, order, next, actor);

    return toResponse(order);
  }

  // ── Helpers ──────────────────────────────────────────────────────────────────

  private void sendLifecycleEmail(User customer, LaundryOrder order,
                                  OrderStatus next, User actor) {
    switch (next) {
      case confirmed ->
              emailService.sendOrderConfirmed(
                      customer.getEmail(), customer.getFullName(), order.getOrderNumber());
      case pickup_assigned ->
              emailService.sendPickupAssigned(
                      customer.getEmail(), customer.getFullName(),
                      order.getOrderNumber(), actor.getFullName());
      case delivery_assigned ->
              emailService.sendDeliveryAssigned(
                      customer.getEmail(), customer.getFullName(),
                      order.getOrderNumber(), actor.getFullName());
      case delivered ->
              emailService.sendOrderDelivered(
                      customer.getEmail(), customer.getFullName(), order.getOrderNumber());
      default ->
              emailService.sendOrderStatusUpdate(
                      customer.getEmail(), customer.getFullName(),
                      order.getOrderNumber(), statusLabel(next),
                      customerNotification(next, order.getOrderNumber()));
    }
  }

  public String statusLabel(OrderStatus status) {
    return switch (status) {
      case pending -> "Order Placed";
      case confirmed -> "Order Confirmed";
      case pickup_assigned -> "Pickup Assigned";
      case picked_up -> "Picked Up";
      case in_wash -> "Washing Started";
      case wash_complete -> "Washing Done";
      case in_dry_clean -> "Dry Cleaning Started";
      case dry_clean_complete -> "Dry Cleaning Done";
      case waiting_for_iron -> "Queued for Ironing";
      case in_iron -> "Ironing Started";
      case iron_complete -> "Ironing Done";
      case ready -> "Ready for Delivery";
      case delivery_assigned -> "Delivery Assigned";
      case out_for_delivery -> "Out for Delivery";
      case delivered -> "Delivered";
      case delivery_failed -> "Delivery Failed";
      case returned -> "Returned";
      case disputed -> "Disputed";
      case cancelled -> "Order Cancelled";
    };
  }

  private LaundryOrder scopedOrder(UUID id) {
    User user = principalService.currentUser();
    LaundryOrder order = orderRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Order not found"));
    if (user.getRole() == UserRole.customer
            && !order.getCustomer().getId().equals(user.getId())) {
      throw new NotFoundException("Order not found");
    }
    return order;
  }

  private void validateTransition(OrderStatus current, OrderStatus next) {
    if (!ALLOWED_TRANSITIONS.getOrDefault(current, Set.of()).contains(next)) {
      throw new BadRequestException(
              "Invalid status transition: " + current + " -> " + next);
    }
  }

  private void addTracking(LaundryOrder order, OrderStatus status, String description,
                           User actor) {
    var tracking = new OrderTracking();
    tracking.setOrder(order);
    tracking.setStatus(status.name());
    tracking.setStatusLabel(statusLabel(status));
    tracking.setDescription(description);
    tracking.setUpdatedBy(actor);
    if (actor != null) {
      tracking.setActorRole(actor.getRole());
    }
    trackingRepository.save(tracking);
  }

  private OrderResponse toResponse(LaundryOrder order) {
    return OrderResponse.from(order,
            orderItemRepository.findByOrderId(order.getId()).stream()
                    .map(OrderItemResponse::from)
                    .toList());
  }

  private String nextOrderNumber() {
    LocalDate today = LocalDate.now(DHAKA);
    Instant start = today.atStartOfDay(DHAKA).toInstant();
    Instant end = today.plusDays(1).atStartOfDay(DHAKA).toInstant();
    long sequence = orderRepository.countByCreatedAtBetween(start, end) + 1;
    return "IRM-" + today.format(DateTimeFormatter.BASIC_ISO_DATE)
            + "-" + String.format("%04d", sequence);
  }

  private String customerNotification(OrderStatus status, String orderNumber) {
    return switch (status) {
      case pending -> "Order placed! #" + orderNumber;
      case confirmed -> "Order confirmed";
      case pickup_assigned -> "Your pickup has been assigned";
      case picked_up -> "Your clothes are picked";
      case in_wash -> "Washing started";
      case wash_complete -> "Washing done";
      case in_dry_clean -> "Dry cleaning started";
      case dry_clean_complete -> "Dry cleaning done";
      case waiting_for_iron -> "Queued for ironing";
      case in_iron -> "Ironing started";
      case iron_complete -> "Ironing done";
      case ready -> "Order ready for delivery";
      case delivery_assigned -> "Delivery has been assigned";
      case out_for_delivery -> "Your order is on the way";
      case delivered -> "Delivered! Rate service";
      case delivery_failed -> "Delivery attempt failed — we'll retry";
      case returned -> "Order returned";
      case disputed -> "Your complaint is under review";
      case cancelled -> "Order cancelled";
    };
  }
}
