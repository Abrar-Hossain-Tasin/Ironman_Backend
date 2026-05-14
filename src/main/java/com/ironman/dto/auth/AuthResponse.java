package com.ironman.dto.auth;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ironman.dto.user.UserSummary;

public record AuthResponse(
    @JsonIgnore
    String accessToken,
    @JsonIgnore
    String refreshToken,
    String tokenType,
    UserSummary user,
    String csrfToken
) {
  public AuthResponse withCsrfToken(String csrfToken) {
    return new AuthResponse(accessToken, refreshToken, tokenType, user, csrfToken);
  }
}
