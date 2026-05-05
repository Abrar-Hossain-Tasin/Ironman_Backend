package com.ironman.dto.pricing;

import com.ironman.model.ServiceCategory;
import java.util.UUID;

public record ServiceCategoryResponse(
    UUID id,
    String name,
    String description,
    String iconUrl,
    int displayOrder
) {
  public static ServiceCategoryResponse from(ServiceCategory category) {
    return new ServiceCategoryResponse(
        category.getId(),
        category.getName(),
        category.getDescription(),
        category.getIconUrl(),
        category.getDisplayOrder()
    );
  }
}
