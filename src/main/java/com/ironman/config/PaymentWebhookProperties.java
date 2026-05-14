package com.ironman.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.payments.webhooks")
public class PaymentWebhookProperties {
  private Duration signatureTolerance = Duration.ofMinutes(5);
  private int maxAttempts = 6;
  private Duration retryBaseDelay = Duration.ofMinutes(5);
  private Map<String, Provider> providers = new HashMap<>();

  @Getter
  @Setter
  public static class Provider {
    private String secret;
    private String signatureHeader;
    private String timestampHeader;
    private String eventIdHeader;
    private String idempotencyHeader;
  }
}
