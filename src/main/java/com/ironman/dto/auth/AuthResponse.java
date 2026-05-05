package com.ironman.dto.auth;

import com.ironman.dto.user.UserSummary;

public record AuthResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    UserSummary user
) {}
