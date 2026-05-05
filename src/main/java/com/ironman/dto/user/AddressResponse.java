package com.ironman.dto.user;

import com.ironman.model.Address;
import java.math.BigDecimal;
import java.util.UUID;

public record AddressResponse(
    UUID id,
    String label,
    String addressLine1,
    String addressLine2,
    String area,
    String city,
    String postalCode,
    BigDecimal latitude,
    BigDecimal longitude,
    boolean defaultAddress
) {
  public static AddressResponse from(Address address) {
    return new AddressResponse(
        address.getId(),
        address.getLabel(),
        address.getAddressLine1(),
        address.getAddressLine2(),
        address.getArea(),
        address.getCity(),
        address.getPostalCode(),
        address.getLatitude(),
        address.getLongitude(),
        address.isDefaultAddress()
    );
  }
}
