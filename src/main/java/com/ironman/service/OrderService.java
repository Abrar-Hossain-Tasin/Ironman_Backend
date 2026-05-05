package com.ironman.service;

import com.ironman.config.BadRequestException;
import com.ironman.config.NotFoundException;
import com.ironman.dto.order.OrderItemResponse;
import com.ironman.dto.order.OrderResponse;
import com.ironman.dto.order.PlaceOrderRequest;
import com.ironman.dto.order.TrackingResponse;
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
      Map.entry(OrderStatus.pending, Set.of(OrderStatus.confirmed, OrderStatus.cancelled)),
      Map.entry(OrderStatus.confirmed, Set.of(OrderStatus.pickup_assigned, OrderStatus.cancelled)),
      Map.entry(OrderStatus.pickup_assigned, Set.of(OrderStatus.picked_up, OrderStatus.cancelled)),
      Map.entry(OrderStatus.picked_up, Set.of(OrderStatus.in_wash, OrderStatus.in_dry_clean, OrderStatus.waiting_for_iron, OrderStatus.in_iron, OrderStatus.ready, OrderStatus.cancelled)),
      Map.entry(OrderStatus.in_wash, Set.of(OrderStatus.wash_complete, OrderStatus.cancelled)),
      Map.entry(OrderStatus.wash_complete, Set.of(OrderStatus.waiting_for_iron, OrderStatus.in_iron, OrderStatus.ready, OrderStatus.cancelled)),
      Map.entry(OrderStatus.in_dry_clean, Set.of(OrderStatus.dry_clean_complete, OrderStatus.cancelled)),
      Map.entry(OrderStatus.dry_clean_complete, Set.of(OrderStatus.waiting_for_iron, OrderStatus.in_iron, OrderStatus.ready, OrderStatus.cancelled)),
      Map.entry(OrderStatus.waiting_for_iron, Set.of(OrderStatus.in_iron, OrderStatus.cancelled)),
      Map.entry(OrderStatus.in_iron, Set.of(OrderStatus.iron_complete, OrderStatus.cancelled)),
      Map.entry(OrderStatus.iron_complete, Set.of(OrderStatus.ready, OrderStatus.cancelled)),
      Map.entry(OrderStatus.ready, Set.of(OrderStatus.delivery_assigned, OrderStatus.cancelled)),
      Map.entry(OrderStatus.delivery_assigned, Set.of(OrderStatus.out_for_delivery, OrderStatus.cancelled)),
      Map.entry(OrderStatus.out_for_delivery, Set.of(OrderStatus.delivered, OrderStatus.cancelled)),
      Map.entry(OrderStatus.delivered, Set.of()),
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

  @Transactional
  public OrderResponse placeOrder(PlaceOrderRequest request) {
    User customer = principalService.currentUser();
    if (customer.getRole() != UserRole.customer) {
      throw new BadRequestException("Only customers can place orders");
    }

    var pickupAddress = addressRepository.findByIdAndUserId(request.pickupAddressId(), customer.getId())
        .orElseThrow(() -> new NotFoundException("Pickup address not found"));
    var deliveryAddress = addressRepository.findByIdAndUserId(request.deliveryAddressId(), customer.getId())
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
      var pricing = servicePricingRepository.findByServiceCategoryIdAndClothingTypeIdAndCurrentTrue(category.getId(), clothingType.getId())
          .orElseThrow(() -> new BadRequestException("Pricing is unavailable for " + category.getName() + " / " + clothingType.getName()));

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

    order.setTotalAmount(total);
    order = orderRepository.save(order);
    addTracking(order, OrderStatus.pending, "Order placed", customer);

    customerProfileRepository.findByUserId(customer.getId()).ifPresent(profile -> {
      profile.setTotalOrders(profile.getTotalOrders() + 1);
      customerProfileRepository.save(profile);
    });

    notificationService.notifyAdmins(
        "New order " + order.getOrderNumber(),
        customer.getFullName() + " placed a BDT " + order.getTotalAmount() + " order.",
        "order_created",
        order.getId()
    );

    return toResponse(order);
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
  public OrderResponse cancel(UUID id) {
    LaundryOrder order = scopedOrder(id);
    if (!Set.of(OrderStatus.pending, OrderStatus.confirmed, OrderStatus.pickup_assigned).contains(order.getStatus())) {
      throw new BadRequestException("Order cannot be cancelled after pickup");
    }
    return updateStatus(order, OrderStatus.cancelled, "Order cancelled", principalService.currentUser());
  }

  @Transactional
  public OrderResponse updateStatus(UUID id, OrderStatus next, String reason) {
    return updateStatus(scopedOrder(id), next, reason, principalService.currentUser());
  }

  @Transactional
  public OrderResponse updateStatus(LaundryOrder order, OrderStatus next, String reason, User actor) {
    if (order.getStatus() == next) {
      return toResponse(order);
    }
    validateTransition(order.getStatus(), next);
    order.setStatus(next);
    order = orderRepository.save(order);
    addTracking(order, next, reason == null || reason.isBlank() ? statusLabel(next) : reason, actor);
    notificationService.notifyUser(
        order.getCustomer(),
        statusLabel(next),
        customerNotification(next, order.getOrderNumber()),
        "order_status",
        order.getId()
    );
    return toResponse(order);
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
      case cancelled -> "Order Cancelled";
    };
  }

  private LaundryOrder scopedOrder(UUID id) {
    User user = principalService.currentUser();
    LaundryOrder order = orderRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Order not found"));
    if (user.getRole() == UserRole.customer && !order.getCustomer().getId().equals(user.getId())) {
      throw new NotFoundException("Order not found");
    }
    return order;
  }

  private void validateTransition(OrderStatus current, OrderStatus next) {
    if (!ALLOWED_TRANSITIONS.getOrDefault(current, Set.of()).contains(next)) {
      throw new BadRequestException("Invalid status transition: " + current + " -> " + next);
    }
  }

  private void addTracking(LaundryOrder order, OrderStatus status, String description, User actor) {
    var tracking = new OrderTracking();
    tracking.setOrder(order);
    tracking.setStatus(status.name());
    tracking.setStatusLabel(statusLabel(status));
    tracking.setDescription(description);
    tracking.setUpdatedBy(actor);
    trackingRepository.save(tracking);
  }

  private OrderResponse toResponse(LaundryOrder order) {
    return OrderResponse.from(order, orderItemRepository.findByOrderId(order.getId()).stream()
        .map(OrderItemResponse::from)
        .toList());
  }

  private String nextOrderNumber() {
    LocalDate today = LocalDate.now(DHAKA);
    Instant start = today.atStartOfDay(DHAKA).toInstant();
    Instant end = today.plusDays(1).atStartOfDay(DHAKA).toInstant();
    long sequence = orderRepository.countByCreatedAtBetween(start, end) + 1;
    return "IRM-" + today.format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + String.format("%04d", sequence);
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
      case cancelled -> "Order cancelled";
    };
  }
}
