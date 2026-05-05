package com.ironman.dto.user;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record AddressRequest(
    @NotBlank String label,
    @NotBlank String addressLine1,
    String addressLine2,
    @NotBlank String area,
    @NotBlank String city,
    String postalCode,
    BigDecimal latitude,
    BigDecimal longitude,
    boolean defaultAddress
) {}
