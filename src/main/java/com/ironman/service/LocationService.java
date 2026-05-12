package com.ironman.service;

import com.ironman.config.BadRequestException;
import com.ironman.config.NotFoundException;
import com.ironman.dto.location.LocationResponse;
import com.ironman.dto.location.LocationUpdateRequest;
import com.ironman.model.AssignmentStatus;
import com.ironman.model.AssignmentType;
import com.ironman.model.DeliveryLocation;
import com.ironman.model.LaundryOrder;
import com.ironman.model.User;
import com.ironman.model.UserRole;
import com.ironman.repository.DeliveryLocationRepository;
import com.ironman.repository.LaundryOrderRepository;
import com.ironman.repository.OrderAssignmentRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocationService {

    private static final List<AssignmentType> TRACKABLE_ASSIGNMENT_TYPES =
            List.of(AssignmentType.pickup, AssignmentType.delivery);
    private static final List<AssignmentStatus> ACTIVE_ASSIGNMENT_STATUSES =
            List.of(AssignmentStatus.accepted, AssignmentStatus.in_progress);

    private final PrincipalService principalService;
    private final DeliveryLocationRepository locationRepository;
    private final LaundryOrderRepository orderRepository;
    private final OrderAssignmentRepository assignmentRepository;

    private final Map<UUID, List<SseEmitter>> subscribers = new ConcurrentHashMap<>();

    @Transactional
    public LocationResponse updateMyLocation(LocationUpdateRequest req) {
        User me = principalService.currentUser();

        DeliveryLocation loc = locationRepository.findByDeliveryManId(me.getId())
                .orElseGet(() -> {
                    var next = new DeliveryLocation();
                    next.setDeliveryMan(me);
                    return next;
                });

        LaundryOrder order = null;
        if (req.orderId() != null) {
            order = orderRepository.findById(req.orderId())
                    .orElseThrow(() -> new NotFoundException("Order not found"));
            boolean assignedToOrder = assignmentRepository
                    .existsByOrderIdAndAssignedToIdAndAssignmentTypeInAndStatusIn(
                            order.getId(),
                            me.getId(),
                            TRACKABLE_ASSIGNMENT_TYPES,
                            ACTIVE_ASSIGNMENT_STATUSES
                    );
            if (!assignedToOrder) {
                throw new BadRequestException(
                        "Location can only be shared for an accepted or active pickup/delivery assignment");
            }
        }

        loc.setLatitude(req.latitude());
        loc.setLongitude(req.longitude());
        loc.setAccuracy(req.accuracy());
        loc.setUpdatedAt(Instant.now());
        loc.setOrder(order);

        loc = locationRepository.save(loc);
        LocationResponse response = LocationResponse.from(loc);

        if (order != null) {
            pushToSubscribers(order.getId(), response);
        }

        return response;
    }

    @Transactional(readOnly = true)
    public List<LocationResponse> allLocations() {
        return locationRepository.findAll().stream()
                .map(LocationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public LocationResponse locationForOrder(UUID orderId) {
        scopedOrder(orderId);
        return locationRepository.findByOrderId(orderId)
                .map(LocationResponse::from)
                .orElseThrow(() -> new NotFoundException(
                        "No live location available for this order yet"));
    }

    @Transactional(readOnly = true)
    public SseEmitter subscribeToOrder(UUID orderId) {
        scopedOrder(orderId);

        var emitter = new SseEmitter(30 * 60 * 1000L);
        subscribers.computeIfAbsent(orderId, key -> new CopyOnWriteArrayList<>()).add(emitter);

        Runnable remove = () -> {
            var list = subscribers.get(orderId);
            if (list != null) {
                list.remove(emitter);
            }
        };
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(error -> remove.run());

        locationRepository.findByOrderId(orderId).ifPresent(loc -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("location")
                        .data(LocationResponse.from(loc)));
            } catch (Exception ignored) {
                remove.run();
            }
        });

        return emitter;
    }

    private LaundryOrder scopedOrder(UUID orderId) {
        User user = principalService.currentUser();
        LaundryOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));

        if (user.getRole() == UserRole.customer
                && !order.getCustomer().getId().equals(user.getId())) {
            throw new NotFoundException("Order not found");
        }

        if (user.getRole() != UserRole.admin && user.getRole() != UserRole.customer) {
            throw new BadRequestException("Live location is only available to admins and the order customer");
        }

        return order;
    }

    private void pushToSubscribers(UUID orderId, LocationResponse payload) {
        var list = subscribers.get(orderId);
        if (list == null || list.isEmpty()) {
            return;
        }

        var dead = new ArrayList<SseEmitter>();
        for (var emitter : list) {
            try {
                emitter.send(SseEmitter.event().name("location").data(payload));
            } catch (Exception ex) {
                dead.add(emitter);
            }
        }
        list.removeAll(dead);
    }
}
