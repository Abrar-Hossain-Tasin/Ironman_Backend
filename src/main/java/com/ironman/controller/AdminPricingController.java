package com.ironman.controller;

import com.ironman.dto.common.ApiMessage;
import com.ironman.dto.pricing.PricingResponse;
import com.ironman.dto.pricing.PricingUpdateRequest;
import com.ironman.service.PricingService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/v1/admin/pricing")
@RequiredArgsConstructor
public class AdminPricingController {
  private final PricingService pricingService;

  @PostMapping
  public PricingResponse setPrice(@Valid @RequestBody PricingUpdateRequest request) {
    return pricingService.setPrice(request);
  }

  @GetMapping("/history")
  public List<PricingResponse> history() {
    return pricingService.history();
  }

  @DeleteMapping("/{id}")
  public ApiMessage deactivate(@PathVariable UUID id) {
    pricingService.deactivate(id);
    return new ApiMessage("Price deactivated");
  }
}
