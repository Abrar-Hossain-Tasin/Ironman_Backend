package com.ironman.controller;

import com.ironman.dto.location.LocationResponse;
import com.ironman.dto.location.LocationUpdateRequest;
import com.ironman.service.LocationService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/location")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    // Delivery man pushes GPS position every ~5 seconds
    @PostMapping("/me")
    @PreAuthorize("hasRole('DELIVERY_MAN')")
    public LocationResponse updateMyLocation(
            @Valid @RequestBody LocationUpdateRequest request) {
        return locationService.updateMyLocation(request);
    }

    // Admin sees all delivery agents on a map
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public List<LocationResponse> allLocations() {
        return locationService.allLocations();
    }

    // Admin or customer polls last known location for one order
    @GetMapping("/orders/{orderId}")
    @PreAuthorize("hasAnyRole('ADMIN','CUSTOMER')")
    public LocationResponse locationForOrder(@PathVariable UUID orderId) {
        return locationService.locationForOrder(orderId);
    }

    // Real-time SSE stream — browser subscribes once, gets pushed updates
    @GetMapping(value = "/orders/{orderId}/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','CUSTOMER')")
    public SseEmitter streamOrderLocation(@PathVariable UUID orderId) {
        return locationService.subscribeToOrder(orderId);
    }
}