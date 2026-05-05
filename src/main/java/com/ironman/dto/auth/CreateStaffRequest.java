package com.ironman.dto.auth;

import com.ironman.model.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateStaffRequest(
    @NotBlank String fullName,
    @Email @NotBlank String email,
    @NotBlank String phone,
    @Size(min = 8) String password,
    @NotNull UserRole role
) {}
