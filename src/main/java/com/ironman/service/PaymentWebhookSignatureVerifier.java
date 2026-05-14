package com.ironman.service;

import com.ironman.config.PaymentWebhookProperties;
import com.ironman.config.UnauthorizedException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentWebhookSignatureVerifier {
  private static final HexFormat HEX = HexFormat.of();

  private final PaymentWebhookProperties properties;

  public VerifiedWebhook verify(String provider, String rawPayload, HttpHeaders headers) {
    String normalizedProvider = normalizeProvider(provider);
    PaymentWebhookProperties.Provider config = providerConfig(normalizedProvider);
    String secret = clean(config.getSecret());
    if (secret == null) {
      throw new UnauthorizedException(
          "Webhook signing secret is not configured for provider " + normalizedProvider);
    }

    String signatureHeader = headerName(
        config.getSignatureHeader(), "X-" + providerLabel(normalizedProvider) + "-Signature");
    String timestampHeader = headerName(
        config.getTimestampHeader(), "X-" + providerLabel(normalizedProvider) + "-Timestamp");
    String eventIdHeader = headerName(
        config.getEventIdHeader(), "X-" + providerLabel(normalizedProvider) + "-Event-Id");
    String idempotencyHeader = headerName(
        config.getIdempotencyHeader(), "X-" + providerLabel(normalizedProvider) + "-Idempotency-Key");

    String signature = clean(headers.getFirst(signatureHeader));
    String timestamp = clean(headers.getFirst(timestampHeader));
    if (signature == null) {
      throw new UnauthorizedException("Missing webhook signature header " + signatureHeader);
    }
    if (timestamp == null) {
      throw new UnauthorizedException("Missing webhook timestamp header " + timestampHeader);
    }
    enforceFreshTimestamp(timestamp);

    String expected = hmacSha256Hex(secret, timestamp + "." + rawPayload);
    if (!constantTimeEquals(expected, normalizeSignature(signature))) {
      throw new UnauthorizedException("Invalid webhook signature");
    }

    String idempotencyKey = firstClean(
        headers.getFirst(idempotencyHeader),
        headers.getFirst("Idempotency-Key"),
        headers.getFirst("X-Idempotency-Key")
    );

    return new VerifiedWebhook(
        normalizedProvider,
        clean(headers.getFirst(eventIdHeader)),
        idempotencyKey,
        signature,
        timestampHeader,
        timestamp,
        signatureHeader,
        payloadSha256(rawPayload)
    );
  }

  private PaymentWebhookProperties.Provider providerConfig(String provider) {
    PaymentWebhookProperties.Provider config = properties.getProviders().get(provider);
    return config == null ? new PaymentWebhookProperties.Provider() : config;
  }

  private void enforceFreshTimestamp(String timestamp) {
    Instant signedAt = parseTimestamp(timestamp);
    Duration drift = Duration.between(signedAt, Instant.now()).abs();
    if (drift.compareTo(properties.getSignatureTolerance()) > 0) {
      throw new UnauthorizedException("Webhook timestamp is outside the replay window");
    }
  }

  private Instant parseTimestamp(String value) {
    try {
      long numeric = Long.parseLong(value);
      return numeric > 9_999_999_999L
          ? Instant.ofEpochMilli(numeric)
          : Instant.ofEpochSecond(numeric);
    } catch (NumberFormatException ignored) {
      try {
        return Instant.parse(value);
      } catch (RuntimeException ex) {
        throw new UnauthorizedException("Invalid webhook timestamp");
      }
    }
  }

  private String hmacSha256Hex(String secret, String body) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      return HEX.formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception ex) {
      throw new IllegalStateException("Could not verify webhook signature", ex);
    }
  }

  private String payloadSha256(String rawPayload) {
    try {
      return HEX.formatHex(
          MessageDigest.getInstance("SHA-256").digest(rawPayload.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception ex) {
      throw new IllegalStateException("Could not hash webhook payload", ex);
    }
  }

  private boolean constantTimeEquals(String expected, String actual) {
    return MessageDigest.isEqual(
        expected.getBytes(StandardCharsets.UTF_8),
        actual.getBytes(StandardCharsets.UTF_8));
  }

  private String normalizeSignature(String signature) {
    String trimmed = signature.trim();
    return trimmed.startsWith("sha256=") ? trimmed.substring("sha256=".length()) : trimmed;
  }

  private String normalizeProvider(String provider) {
    String normalized = clean(provider);
    if (normalized == null) {
      throw new UnauthorizedException("Payment provider is required");
    }
    return normalized.toLowerCase();
  }

  private String providerLabel(String provider) {
    return switch (provider) {
      case "bkash" -> "BKash";
      case "nagad" -> "Nagad";
      case "rocket" -> "Rocket";
      case "card" -> "Card";
      default -> provider;
    };
  }

  private String headerName(String configured, String fallback) {
    String value = clean(configured);
    return value == null ? fallback : value;
  }

  private String firstClean(String... values) {
    for (String value : values) {
      String cleaned = clean(value);
      if (cleaned != null) {
        return cleaned;
      }
    }
    return null;
  }

  private String clean(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  public record VerifiedWebhook(
      String provider,
      String eventId,
      String idempotencyKey,
      String signature,
      String timestampHeader,
      String timestamp,
      String signatureHeader,
      String payloadSha256
  ) {
  }
}
