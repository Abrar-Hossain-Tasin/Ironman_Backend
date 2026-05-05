package com.ironman.dto.user;

import jakarta.validation.constraints.NotBlank;

public record UpdateProfileRequest(
    @NotBlank String fullName,
    @NotBlank String phone,
    String profilePictureUrl
) {}
