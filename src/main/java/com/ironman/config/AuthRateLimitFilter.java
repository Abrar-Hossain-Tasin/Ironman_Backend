package com.ironman.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Self-contained sliding-window rate limiter for the auth endpoints. Keyed by
 * client IP + endpoint so credential-stuffing on /auth/login doesn't blow
 * through /auth/register's budget and vice versa. Uses an in-memory map —
 * fine for a single-instance deployment; swap for Redis/bucket4j if we scale
 * horizontally.
 *
 * <p>Limits (per IP per endpoint):
 * <ul>
 *   <li>/auth/login            — 10 attempts / minute</li>
 *   <li>/auth/register         — 5 attempts / minute</li>
 *   <li>/auth/forgot-password  — 3 attempts / minute</li>
 *   <li>/auth/send-verification — 3 attempts / minute</li>
 *   <li>/auth/reset-password   — 5 attempts / minute</li>
 *   <li>/auth/verify-email     — 10 attempts / minute</li>
 * </ul>
 */
@Component
@Order(0)
public class AuthRateLimitFilter extends OncePerRequestFilter {
  private static final Duration WINDOW = Duration.ofMinutes(1);

  // path → max attempts per WINDOW per IP. Anything not in this map is not
  // rate-limited by this filter.
  private static final Map<String, Integer> LIMITS = Map.of(
      "/api/v1/auth/login", 10,
      "/api/v1/auth/register", 5,
      "/api/v1/auth/forgot-password", 3,
      "/api/v1/auth/send-verification", 3,
      "/api/v1/auth/reset-password", 5,
      "/api/v1/auth/verify-email", 10
  );

  private final Map<String, Deque<Instant>> hits = new ConcurrentHashMap<>();

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    Integer limit = LIMITS.get(request.getRequestURI());
    if (limit == null || !"POST".equalsIgnoreCase(request.getMethod())) {
      chain.doFilter(request, response);
      return;
    }

    String key = clientIp(request) + "|" + request.getRequestURI();
    Deque<Instant> bucket = hits.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>());
    Instant now = Instant.now();
    Instant cutoff = now.minus(WINDOW);

    // Evict expired entries. Done lazily on every request to keep memory bounded
    // without spinning up a background thread.
    while (!bucket.isEmpty() && bucket.peekFirst().isBefore(cutoff)) {
      bucket.pollFirst();
    }

    if (bucket.size() >= limit) {
      long retryAfter = WINDOW.minus(Duration.between(bucket.peekFirst(), now)).getSeconds();
      response.setStatus(429);
      response.setHeader("Retry-After", String.valueOf(Math.max(1, retryAfter)));
      response.setContentType("application/json");
      response.getWriter().write(
          "{\"title\":\"Too Many Requests\",\"status\":429,"
              + "\"detail\":\"Too many attempts. Please wait " + Math.max(1, retryAfter)
              + " seconds and try again.\"}");
      return;
    }

    bucket.offerLast(now);
    chain.doFilter(request, response);
  }

  /** Honours X-Forwarded-For when the app sits behind a reverse proxy. */
  private static String clientIp(HttpServletRequest request) {
    String xff = request.getHeader("X-Forwarded-For");
    if (xff != null && !xff.isBlank()) {
      int comma = xff.indexOf(',');
      return (comma > 0 ? xff.substring(0, comma) : xff).trim();
    }
    String real = request.getHeader("X-Real-IP");
    if (real != null && !real.isBlank()) {
      return real.trim();
    }
    return request.getRemoteAddr();
  }
}
