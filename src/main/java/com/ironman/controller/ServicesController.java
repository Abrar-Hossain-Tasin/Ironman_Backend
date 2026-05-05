package com.ironman.controller;

import com.ironman.dto.pricing.ClothingTypeResponse;
import com.ironman.dto.pricing.PricingResponse;
import com.ironman.dto.pricing.ServiceCategoryResponse;
import com.ironman.service.PricingService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/services")
@RequiredArgsConstructor
public class ServicesController {
  private final PricingService pricingService;

  @GetMapping("/categories")
  public List<ServiceCategoryResponse> categories() {
    return pricingService.activeCategories();
  }

  @GetMapping("/clothing-types")
  public List<ClothingTypeResponse> clothingTypes() {
    return pricingService.activeClothingTypes();
  }

  @GetMapping("/pricing")
  public List<PricingResponse> pricing() {
    return pricingService.currentPricing();
  }

  @GetMapping("/pricing/{categoryId}/{clothingTypeId}")
  public PricingResponse pricingCell(@PathVariable UUID categoryId, @PathVariable UUID clothingTypeId) {
    return pricingService.currentPrice(categoryId, clothingTypeId);
  }
}
