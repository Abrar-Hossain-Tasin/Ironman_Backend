package com.ironman.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Getter @Setter @NoArgsConstructor
@Entity
@Table(name = "delivery_locations")
public class DeliveryLocation {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // One row per delivery agent (unique)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "delivery_man_id", nullable = false, unique = true)
    private User deliveryMan;

    // Which order they are currently working on (null = idle)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private LaundryOrder order;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(precision = 8, scale = 2)
    private BigDecimal accuracy;    // device GPS accuracy in metres

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}