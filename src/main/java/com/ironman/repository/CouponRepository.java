package com.ironman.repository;

import com.ironman.model.Coupon;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponRepository extends JpaRepository<Coupon, UUID> {
  Optional<Coupon> findByCodeIgnoreCase(String code);

  List<Coupon> findAllByOrderByCreatedAtDesc();
}
