package com.ironman.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class CsrfProtectionFilter extends OncePerRequestFilter {
  private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String method = request.getMethod();
    if (SAFE_METHODS.contains(method.toUpperCase())) return true;

    String path = normalizedPath(request);
    return !path.startsWith("/api/v1/")
        || path.equals("/api/v1/auth/csrf")
        || path.equals("/api/v1/payment-webhooks")
        || path.startsWith("/api/v1/payment-webhooks/");
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String path = normalizedPath(request);
    boolean hasCookieAuth = hasCookie(request, AuthCookieService.ACCESS_COOKIE)
        || hasCookie(request, AuthCookieService.REFRESH_COOKIE);
    boolean authStateEndpoint = path.startsWith("/api/v1/auth/");
    boolean bearerOnlyClient = hasBearerToken(request) && !hasCookieAuth;

    if (bearerOnlyClient && !authStateEndpoint) {
      filterChain.doFilter(request, response);
      return;
    }

    String csrfCookie = cookieValue(request, AuthCookieService.CSRF_COOKIE);
    String csrfHeader = request.getHeader(AuthCookieService.CSRF_HEADER);
    if (!validCsrfPair(csrfCookie, csrfHeader)) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF token");
      return;
    }

    filterChain.doFilter(request, response);
  }

  private String normalizedPath(HttpServletRequest request) {
    String path = request.getRequestURI();
    String contextPath = request.getContextPath();
    if (contextPath != null && !contextPath.isBlank() && path.startsWith(contextPath)) {
      return path.substring(contextPath.length());
    }
    return path;
  }

  private boolean hasBearerToken(HttpServletRequest request) {
    String header = request.getHeader("Authorization");
    return header != null && header.startsWith("Bearer ");
  }

  private boolean hasCookie(HttpServletRequest request, String name) {
    return cookieValue(request, name) != null;
  }

  private String cookieValue(HttpServletRequest request, String name) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null) return null;
    for (Cookie cookie : cookies) {
      if (name.equals(cookie.getName())
          && cookie.getValue() != null
          && !cookie.getValue().isBlank()) {
        return cookie.getValue();
      }
    }
    return null;
  }

  private boolean validCsrfPair(String csrfCookie, String csrfHeader) {
    if (csrfCookie == null || csrfHeader == null || csrfHeader.isBlank()) return false;
    byte[] cookieBytes = csrfCookie.getBytes(StandardCharsets.UTF_8);
    byte[] headerBytes = csrfHeader.getBytes(StandardCharsets.UTF_8);
    return MessageDigest.isEqual(cookieBytes, headerBytes);
  }
}
