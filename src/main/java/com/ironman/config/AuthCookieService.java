package com.ironman.config;

import com.ironman.dto.auth.AuthResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthCookieService {
  public static final String ACCESS_COOKIE = "ironman_access_token";
  public static final String REFRESH_COOKIE = "ironman_refresh_token";
  public static final String CSRF_COOKIE = "ironman_csrf_token";
  public static final String CSRF_HEADER = "X-CSRF-Token";
  private static final int CSRF_TOKEN_BYTES = 32;
  private final SecureRandom secureRandom = new SecureRandom();

  @Value("${app.jwt.access-token-minutes}")
  private long accessTokenMinutes;

  @Value("${app.jwt.refresh-token-days}")
  private long refreshTokenDays;

  @Value("${app.auth.cookies.secure:false}")
  private boolean secureCookies;

  @Value("${app.auth.cookies.same-site:Lax}")
  private String sameSite;

  public void writeAuthCookies(HttpServletResponse response, AuthResponse auth) {
    response.addHeader(HttpHeaders.SET_COOKIE, cookie(ACCESS_COOKIE, auth.accessToken(), Duration.ofMinutes(accessTokenMinutes), "/").toString());
    response.addHeader(HttpHeaders.SET_COOKIE, cookie(REFRESH_COOKIE, auth.refreshToken(), Duration.ofDays(refreshTokenDays), "/api/v1/auth").toString());
  }

  public void writeAuthCookies(HttpServletResponse response, AuthResponse auth, String csrfToken) {
    writeAuthCookies(response, auth);
    writeCsrfCookie(response, csrfToken);
  }

  public String newCsrfToken() {
    byte[] bytes = new byte[CSRF_TOKEN_BYTES];
    secureRandom.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  public void writeCsrfCookie(HttpServletResponse response, String csrfToken) {
    response.addHeader(HttpHeaders.SET_COOKIE, cookie(CSRF_COOKIE, csrfToken, Duration.ofDays(refreshTokenDays), "/").toString());
    response.setHeader(CSRF_HEADER, csrfToken);
  }

  public void clearAuthCookies(HttpServletResponse response) {
    response.addHeader(HttpHeaders.SET_COOKIE, cookie(ACCESS_COOKIE, "", Duration.ZERO, "/").toString());
    response.addHeader(HttpHeaders.SET_COOKIE, cookie(REFRESH_COOKIE, "", Duration.ZERO, "/api/v1/auth").toString());
    response.addHeader(HttpHeaders.SET_COOKIE, cookie(CSRF_COOKIE, "", Duration.ZERO, "/").toString());
  }

  private ResponseCookie cookie(String name, String value, Duration maxAge, String path) {
    return ResponseCookie.from(name, value)
        .httpOnly(true)
        .secure(secureCookies)
        .sameSite(sameSite)
        .path(path)
        .maxAge(maxAge)
        .build();
  }
}
