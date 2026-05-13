package com.ironman.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetStaffPasswordRequest(
    @NotBlank @Size(min = 8, message = "Password must be at least 8 characters") String newPassword
) {}
