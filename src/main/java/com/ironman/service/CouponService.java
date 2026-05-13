package com.ironman.service;

import com.ironman.config.BadRequestException;
import com.ironman.config.NotFoundException;
import com.ironman.dto.order.CouponRequest;
import com.ironman.dto.order.CouponResponse;
import com.ironman.model.Coupon;
import com.ironman.model.CouponRedemption;
import com.ironman.model.DiscountType;
import com.ironman.model.LaundryOrder;
import com.ironman.model.User;
import com.ironman.repository.CouponRedemptionRepository;
import com.ironman.repository.CouponRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CouponService {

  private final CouponRepository couponRepository;
  private final CouponRedemptionRepository redemptionRepository;

  public record AppliedCoupon(Coupon coupon, BigDecimal discountAmount) {}

  /**
   * Verifies a coupon code against the given subtotal and customer, returning
   * the resolved discount. Throws BadRequestException with a user-readable
   * message for every failure case.
   */
  @Transactional(readOnly = true)
  public AppliedCoupon validate(String code, BigDecimal subtotal, User customer) {
    if (code == null || code.isBlank()) {
      return null;
    }
    Coupon coupon = couponRepository.findByCodeIgnoreCase(code.trim())
        .orElseThrow(() -> new BadRequestException("Invalid coupon code"));

    Instant now = Instant.now();
    if (!coupon.isActive()) {
      throw new BadRequestException("Coupon is not active");
    }
    if (coupon.getValidFrom() != null && now.isBefore(coupon.getValidFrom())) {
      throw new BadRequestException("Coupon is not yet valid");
    }
    if (coupon.getValidTo() != null && now.isAfter(coupon.getValidTo())) {
      throw new BadRequestException("Coupon has expired");
    }
    if (coupon.getMaxUses() != null && coupon.getCurrentUses() >= coupon.getMaxUses()) {
      throw new BadRequestException("Coupon usage limit reached");
    }
    if (subtotal.compareTo(coupon.getMinOrderAmount()) < 0) {
      throw new BadRequestException(
          "Minimum order amount for this coupon is BDT " + coupon.getMinOrderAmount());
    }
    long redemptions = redemptionRepository.countByCouponIdAndCustomerId(
        coupon.getId(), customer.getId());
    if (redemptions >= coupon.getMaxUsesPerUser()) {
      throw new BadRequestException("You have already used this coupon");
    }

    BigDecimal discount;
    if (coupon.getDiscountType() == DiscountType.percent) {
      discount = subtotal.multiply(coupon.getDiscountValue())
          .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
      if (coupon.getMaxDiscountAmount() != null
          && discount.compareTo(coupon.getMaxDiscountAmount()) > 0) {
        discount = coupon.getMaxDiscountAmount();
      }
    } else {
      discount = coupon.getDiscountValue();
    }
    if (discount.compareTo(subtotal) > 0) {
      discount = subtotal;
    }
    return new AppliedCoupon(coupon, discount.setScale(2, RoundingMode.HALF_UP));
  }

  @Transactional
  public void recordRedemption(AppliedCoupon applied, LaundryOrder order, User customer) {
    if (applied == null) {
      return;
    }
    Coupon coupon = applied.coupon();
    coupon.setCurrentUses(coupon.getCurrentUses() + 1);
    coupon.setUpdatedAt(Instant.now());
    couponRepository.save(coupon);

    var redemption = new CouponRedemption();
    redemption.setCoupon(coupon);
    redemption.setOrder(order);
    redemption.setCustomer(customer);
    redemption.setDiscountAmount(applied.discountAmount());
    redemptionRepository.save(redemption);
  }

  @Transactional
  public CouponResponse create(CouponRequest request) {
    if (couponRepository.findByCodeIgnoreCase(request.code()).isPresent()) {
      throw new BadRequestException("Coupon code already exists");
    }
    Coupon coupon = new Coupon();
    apply(coupon, request);
    return CouponResponse.from(couponRepository.save(coupon));
  }

  @Transactional
  public CouponResponse update(UUID id, CouponRequest request) {
    Coupon coupon = couponRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Coupon not found"));
    apply(coupon, request);
    coupon.setUpdatedAt(Instant.now());
    return CouponResponse.from(couponRepository.save(coupon));
  }

  @Transactional
  public void delete(UUID id) {
    Coupon coupon = couponRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Coupon not found"));
    coupon.setActive(false);
    coupon.setUpdatedAt(Instant.now());
    couponRepository.save(coupon);
  }

  @Transactional(readOnly = true)
  public List<CouponResponse> list() {
    return couponRepository.findAllByOrderByCreatedAtDesc().stream()
        .map(CouponResponse::from).toList();
  }

  /**
   * Coupons a customer should be allowed to discover from the UI — active,
   * within their validity window, and with global capacity remaining. Filters
   * out coupons the customer has already maxed out (per-user limit) and any
   * coupon without a description (those tend to be internal / admin-only).
   */
  @Transactional(readOnly = true)
  public List<CouponResponse> activeForCustomer(User customer) {
    Instant now = Instant.now();
    return couponRepository.findAllByOrderByCreatedAtDesc().stream()
        .filter(Coupon::isActive)
        .filter(c -> c.getDescription() != null && !c.getDescription().isBlank())
        .filter(c -> c.getValidFrom() == null || !now.isBefore(c.getValidFrom()))
        .filter(c -> c.getValidTo() == null || !now.isAfter(c.getValidTo()))
        .filter(c -> c.getMaxUses() == null || c.getCurrentUses() < c.getMaxUses())
        .filter(c -> {
          long used = redemptionRepository.countByCouponIdAndCustomerId(c.getId(), customer.getId());
          return used < c.getMaxUsesPerUser();
        })
        .map(CouponResponse::from)
        .toList();
  }

  private void apply(Coupon coupon, CouponRequest request) {
    coupon.setCode(request.code().trim().toUpperCase());
    coupon.setDescription(request.description());
    coupon.setDiscountType(request.discountType());
    coupon.setDiscountValue(request.discountValue());
    coupon.setMinOrderAmount(request.minOrderAmount() == null
        ? BigDecimal.ZERO : request.minOrderAmount());
    coupon.setMaxDiscountAmount(request.maxDiscountAmount());
    coupon.setValidFrom(request.validFrom());
    coupon.setValidTo(request.validTo());
    coupon.setMaxUses(request.maxUses());
    coupon.setMaxUsesPerUser(request.maxUsesPerUser() == null
        ? 1 : request.maxUsesPerUser());
    if (request.active() != null) {
      coupon.setActive(request.active());
    }
  }
}
