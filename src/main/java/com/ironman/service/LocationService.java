package com.ironman.service;

import com.ironman.config.NotFoundException;
import com.ironman.dto.location.LocationResponse;
import com.ironman.dto.location.LocationUpdateRequest;
import com.ironman.model.DeliveryLocation;
import com.ironman.model.User;
import com.ironman.repository.DeliveryLocationRepository;
import com.ironman.repository.LaundryOrderRepository;
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

    private final PrincipalService principalService;
    private final DeliveryLocationRepository locationRepository;
    private final LaundryOrderRepository orderRepository;

    // In-memory SSE subscribers per orderId
    private final Map<UUID, List<SseEmitter>> subscribers = new ConcurrentHashMap<>();

    // ── Delivery man: push location ────────────────────────────────────────────

    @Transactional
    public LocationResponse updateMyLocation(LocationUpdateRequest req) {
        User me = principalService.currentUser();

        DeliveryLocation loc = locationRepository.findByDeliveryManId(me.getId())
                .orElseGet(() -> {
                    var l = new DeliveryLocation();
                    l.setDeliveryMan(me);
                    return l;
                });

        loc.setLatitude(req.latitude());
        loc.setLongitude(req.longitude());
        loc.setAccuracy(req.accuracy());
        loc.setUpdatedAt(Instant.now());
        loc.setOrder(req.orderId() == null ? null :
                orderRepository.findById(req.orderId())
                        .orElseThrow(() -> new NotFoundException("Order not found")));

        loc = locationRepository.save(loc);
        LocationResponse response = LocationResponse.from(loc);

        // Push to SSE listeners for this order
        if (req.orderId() != null) pushToSubscribers(req.orderId(), response);

        return response;
    }

    // ── Admin: all agents on map ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<LocationResponse> allLocations() {
        return locationRepository.findAll().stream()
                .map(LocationResponse::from).toList();
    }

    // ── Admin/Customer: one order's agent location ─────────────────────────────

    @Transactional(readOnly = true)
    public LocationResponse locationForOrder(UUID orderId) {
        return locationRepository.findByOrderId(orderId)
                .map(LocationResponse::from)
                .orElseThrow(() -> new NotFoundException(
                        "No live location available for this order yet"));
    }

    // ── SSE: subscribe to real-time updates ────────────────────────────────────

    public SseEmitter subscribeToOrder(UUID orderId) {
        var emitter = new SseEmitter(5 * 60 * 1000L); // 5 min timeout

        subscribers.computeIfAbsent(orderId,
                k -> new CopyOnWriteArrayList<>()).add(emitter);

        Runnable remove = () -> {
            var list = subscribers.get(orderId);
            if (list != null) list.remove(emitter);
        };
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(e -> remove.run());

        // Send current location immediately on connect
        locationRepository.findByOrderId(orderId).ifPresent(loc -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("location").data(LocationResponse.from(loc)));
            } catch (Exception ignored) {}
        });
        return emitter;
    }

    private void pushToSubscribers(UUID orderId, LocationResponse payload) {
        var list = subscribers.get(orderId);
        if (list == null || list.isEmpty()) return;
        var dead = new ArrayList<SseEmitter>();
        for (var e : list) {
            try { e.send(SseEmitter.event().name("location").data(payload)); }
            catch (Exception ex) { dead.add(e); }
        }
        list.removeAll(dead);
    }
}