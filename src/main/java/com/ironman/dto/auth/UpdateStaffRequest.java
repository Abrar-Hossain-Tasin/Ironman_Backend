package com.ironman.dto.auth;

import com.ironman.model.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Admin-side update for a staff account. Email + role can be changed; password
 * is set via the separate reset flow ({@code /admin/staff/{id}/reset-password}).
 */
public record UpdateStaffRequest(
    @NotBlank String fullName,
    @NotBlank @Email String email,
    @NotBlank String phone,
    @NotNull UserRole role,
    boolean active
) {}
