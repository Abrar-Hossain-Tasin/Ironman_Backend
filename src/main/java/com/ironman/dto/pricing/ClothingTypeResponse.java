package com.ironman.dto.pricing;

import com.ironman.model.ClothingType;
import java.util.UUID;

public record ClothingTypeResponse(
    UUID id,
    String name,
    String iconUrl,
    int displayOrder
) {
  public static ClothingTypeResponse from(ClothingType clothingType) {
    return new ClothingTypeResponse(
        clothingType.getId(),
        clothingType.getName(),
        clothingType.getIconUrl(),
        clothingType.getDisplayOrder()
    );
  }
}
