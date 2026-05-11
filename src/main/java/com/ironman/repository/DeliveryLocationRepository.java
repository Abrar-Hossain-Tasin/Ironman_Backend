package com.ironman.repository;

import com.ironman.model.DeliveryLocation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryLocationRepository
        extends JpaRepository<DeliveryLocation, UUID> {

    Optional<DeliveryLocation> findByDeliveryManId(UUID deliveryManId);
    Optional<DeliveryLocation> findByOrderId(UUID orderId);
    List<DeliveryLocation> findAll();
}