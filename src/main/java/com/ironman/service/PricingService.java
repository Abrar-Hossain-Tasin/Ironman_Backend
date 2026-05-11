//package com.ironman.service;
//
//import com.ironman.config.NotFoundException;
//import com.ironman.dto.pricing.ClothingTypeResponse;
//import com.ironman.dto.pricing.PricingResponse;
//import com.ironman.dto.pricing.PricingUpdateRequest;
//import com.ironman.dto.pricing.ServiceCategoryResponse;
//import com.ironman.model.ServicePricing;
//import com.ironman.model.User;
//import com.ironman.repository.ClothingTypeRepository;
//import com.ironman.repository.ServiceCategoryRepository;
//import com.ironman.repository.ServicePricingRepository;
//import java.time.LocalDate;
//import java.util.List;
//import java.util.UUID;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//@Service
//@RequiredArgsConstructor
//public class PricingService {
//  private final ServiceCategoryRepository serviceCategoryRepository;
//  private final ClothingTypeRepository clothingTypeRepository;
//  private final ServicePricingRepository servicePricingRepository;
//  private final PrincipalService principalService;
//
//  @Transactional(readOnly = true)
//  public List<ServiceCategoryResponse> activeCategories() {
//    return serviceCategoryRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc().stream()
//        .map(ServiceCategoryResponse::from)
//        .toList();
//  }
//
//  @Transactional(readOnly = true)
//  public List<ClothingTypeResponse> activeClothingTypes() {
//    return clothingTypeRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc().stream()
//        .map(ClothingTypeResponse::from)
//        .toList();
//  }
//
//  @Transactional(readOnly = true)
//  public List<PricingResponse> currentPricing() {
//    return servicePricingRepository.findByCurrentTrueOrderByClothingTypeDisplayOrderAscServiceCategoryDisplayOrderAsc()
//        .stream()
//        .map(PricingResponse::from)
//        .toList();
//  }
//
//  @Transactional(readOnly = true)
//  public PricingResponse currentPrice(UUID categoryId, UUID clothingTypeId) {
//    return servicePricingRepository.findByServiceCategoryIdAndClothingTypeIdAndCurrentTrue(categoryId, clothingTypeId)
//        .map(PricingResponse::from)
//        .orElseThrow(() -> new NotFoundException("Current price not found"));
//  }
//
//  @Transactional(readOnly = true)
//  public List<PricingResponse> history() {
//    return servicePricingRepository.findByOrderByCreatedAtDesc().stream()
//        .map(PricingResponse::from)
//        .toList();
//  }
//
//  @Transactional
//  public PricingResponse setPrice(PricingUpdateRequest request) {
//    User admin = principalService.currentUser();
//    var category = serviceCategoryRepository.findById(request.serviceCategoryId())
//        .orElseThrow(() -> new NotFoundException("Service category not found"));
//    var clothingType = clothingTypeRepository.findById(request.clothingTypeId())
//        .orElseThrow(() -> new NotFoundException("Clothing type not found"));
//
//    servicePricingRepository.findByServiceCategoryIdAndClothingTypeIdAndCurrentTrue(category.getId(), clothingType.getId())
//        .ifPresent(existing -> {
//          existing.setCurrent(false);
//          existing.setEffectiveTo(LocalDate.now().minusDays(1));
//          servicePricingRepository.save(existing);
//        });
//
//    var pricing = new ServicePricing();
//    pricing.setServiceCategory(category);
//    pricing.setClothingType(clothingType);
//    pricing.setPrice(request.price());
//    pricing.setEffectiveFrom(request.effectiveFrom() == null ? LocalDate.now() : request.effectiveFrom());
//    pricing.setSetByAdmin(admin);
//    return PricingResponse.from(servicePricingRepository.save(pricing));
//  }
//
//  @Transactional
//  public void deactivate(UUID id) {
//    ServicePricing pricing = servicePricingRepository.findById(id)
//        .orElseThrow(() -> new NotFoundException("Price not found"));
//    pricing.setCurrent(false);
//    pricing.setEffectiveTo(LocalDate.now());
//    servicePricingRepository.save(pricing);
//  }
//}
package com.ironman.service;

import com.ironman.config.NotFoundException;
import com.ironman.dto.pricing.ClothingTypeResponse;
import com.ironman.dto.pricing.PricingResponse;
import com.ironman.dto.pricing.PricingUpdateRequest;
import com.ironman.dto.pricing.ServiceCategoryResponse;
import com.ironman.model.ServicePricing;
import com.ironman.model.User;
import com.ironman.repository.ClothingTypeRepository;
import com.ironman.repository.ServiceCategoryRepository;
import com.ironman.repository.ServicePricingRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PricingService {
  private final ServiceCategoryRepository serviceCategoryRepository;
  private final ClothingTypeRepository clothingTypeRepository;
  private final ServicePricingRepository servicePricingRepository;
  private final PrincipalService principalService;

  @Transactional(readOnly = true)
  public List<ServiceCategoryResponse> activeCategories() {
    return serviceCategoryRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc().stream()
            .map(ServiceCategoryResponse::from)
            .toList();
  }

  @Transactional(readOnly = true)
  public List<ClothingTypeResponse> activeClothingTypes() {
    return clothingTypeRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc().stream()
            .map(ClothingTypeResponse::from)
            .toList();
  }

  @Transactional(readOnly = true)
  public List<PricingResponse> currentPricing() {
    return servicePricingRepository.findByCurrentTrueOrderByClothingTypeDisplayOrderAscServiceCategoryDisplayOrderAsc()
            .stream()
            .map(PricingResponse::from)
            .toList();
  }

  @Transactional(readOnly = true)
  public PricingResponse currentPrice(UUID categoryId, UUID clothingTypeId) {
    return servicePricingRepository.findByServiceCategoryIdAndClothingTypeIdAndCurrentTrue(categoryId, clothingTypeId)
            .map(PricingResponse::from)
            .orElseThrow(() -> new NotFoundException("Current price not found"));
  }

  @Transactional(readOnly = true)
  public List<PricingResponse> history() {
    return servicePricingRepository.findByOrderByCreatedAtDesc().stream()
            .map(PricingResponse::from)
            .toList();
  }

  @Transactional
  public PricingResponse setPrice(PricingUpdateRequest request) {
    User admin = principalService.currentUser();
    var category = serviceCategoryRepository.findById(request.serviceCategoryId())
            .orElseThrow(() -> new NotFoundException("Service category not found"));
    var clothingType = clothingTypeRepository.findById(request.clothingTypeId())
            .orElseThrow(() -> new NotFoundException("Clothing type not found"));

    servicePricingRepository.findByServiceCategoryIdAndClothingTypeIdAndCurrentTrue(category.getId(), clothingType.getId())
            .ifPresent(existing -> {
              existing.setCurrent(false);
              existing.setEffectiveTo(LocalDate.now().minusDays(1));
              servicePricingRepository.saveAndFlush(existing); // flush immediately so the DB sees is_current=false
              // before the new row (is_current=true) is inserted,
              // avoiding a uq_service_pricing_current violation
            });

    var pricing = new ServicePricing();
    pricing.setServiceCategory(category);
    pricing.setClothingType(clothingType);
    pricing.setPrice(request.price());
    pricing.setEffectiveFrom(request.effectiveFrom() == null ? LocalDate.now() : request.effectiveFrom());
    pricing.setSetByAdmin(admin);
    return PricingResponse.from(servicePricingRepository.save(pricing));
  }

  @Transactional
  public void deactivate(UUID id) {
    ServicePricing pricing = servicePricingRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Price not found"));
    pricing.setCurrent(false);
    pricing.setEffectiveTo(LocalDate.now());
    servicePricingRepository.save(pricing);
  }
}