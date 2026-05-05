package com.ironman.repository;

import com.ironman.model.ServicePricing;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServicePricingRepository extends JpaRepository<ServicePricing, UUID> {
  List<ServicePricing> findByCurrentTrueOrderByClothingTypeDisplayOrderAscServiceCategoryDisplayOrderAsc();

  List<ServicePricing> findByOrderByCreatedAtDesc();

  Optional<ServicePricing> findByServiceCategoryIdAndClothingTypeIdAndCurrentTrue(UUID categoryId, UUID clothingTypeId);
}
