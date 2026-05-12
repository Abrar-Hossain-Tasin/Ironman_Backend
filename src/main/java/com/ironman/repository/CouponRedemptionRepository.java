package com.ironman.repository;

import com.ironman.model.CouponRedemption;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponRedemptionRepository extends JpaRepository<CouponRedemption, UUID> {
  long countByCouponIdAndCustomerId(UUID couponId, UUID customerId);
}
