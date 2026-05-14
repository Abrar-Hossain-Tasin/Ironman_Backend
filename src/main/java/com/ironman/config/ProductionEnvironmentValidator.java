package com.ironman.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.springframework.core.env.Environment;

public final class ProductionEnvironmentValidator {
  private ProductionEnvironmentValidator() {
  }

  public static void validate(Environment environment) {
    if (!shouldValidate(environment)) {
      return;
    }

    List<String> errors = new ArrayList<>();
    requireDatabase(environment, errors);
    requireSecret(environment, "app.jwt.secret", "JWT_SECRET", errors);
    requireAllowedOrigins(environment, errors);
    requireTrue(environment, "app.auth.cookies.secure", "AUTH_COOKIE_SECURE", errors);
    requireSameSiteNone(environment, errors);
    requireSecret(environment, "app.payments.webhook-secret", "PAYMENT_WEBHOOK_SECRET", errors);
    requireRawSecret(environment, "PAYMENT_WEBHOOK_BKASH_SECRET", errors);
    requireRawSecret(environment, "PAYMENT_WEBHOOK_NAGAD_SECRET", errors);
    requireRawSecret(environment, "PAYMENT_WEBHOOK_ROCKET_SECRET", errors);
    requireRawSecret(environment, "PAYMENT_WEBHOOK_CARD_SECRET", errors);
    requireSecret(environment, "sentry.dsn", "SENTRY_DSN", errors);
    requireMailIfEnabled(environment, errors);

    if (!errors.isEmpty()) {
      throw new IllegalStateException(
          "Production environment is not ready:\n- " + String.join("\n- ", errors));
    }
  }

  private static boolean shouldValidate(Environment environment) {
    boolean prodProfile = Arrays.stream(environment.getActiveProfiles())
        .anyMatch(profile -> profile.equalsIgnoreCase("prod") || profile.equalsIgnoreCase("production"));
    boolean strict = Boolean.parseBoolean(environment.getProperty("STRICT_ENV_VALIDATION", "false"));
    boolean render = hasText(environment.getProperty("RENDER_SERVICE_ID"));
    return prodProfile || strict || render;
  }

  private static void requireDatabase(Environment environment, List<String> errors) {
    String url = property(environment, "spring.datasource.url");
    String username = property(environment, "spring.datasource.username");
    String password = property(environment, "spring.datasource.password");

    if (isPlaceholder(url)) {
      errors.add("SPRING_DATASOURCE_URL or SUPABASE_DB_URL must be set.");
    } else if (containsLocalhost(url)) {
      errors.add("SPRING_DATASOURCE_URL must not point to localhost in production.");
    }

    if (isPlaceholder(username)) {
      errors.add("SPRING_DATASOURCE_USERNAME or SUPABASE_DB_USERNAME must be set.");
    }
    if (isPlaceholder(password)) {
      errors.add("SPRING_DATASOURCE_PASSWORD or SUPABASE_DB_PASSWORD must be set.");
    }
  }

  private static void requireAllowedOrigins(Environment environment, List<String> errors) {
    String origins = property(environment, "app.cors.allowed-origins");
    if (isPlaceholder(origins)) {
      errors.add("ALLOWED_ORIGINS must include the deployed frontend origin.");
      return;
    }

    List<String> unsafeOrigins = Arrays.stream(origins.split(","))
        .map(String::trim)
        .filter(origin -> origin.equals("*") || containsLocalhost(origin) || !origin.startsWith("https://"))
        .toList();

    if (!unsafeOrigins.isEmpty()) {
      errors.add("ALLOWED_ORIGINS must only contain HTTPS production origins: " + unsafeOrigins);
    }
  }

  private static void requireTrue(
      Environment environment,
      String propertyKey,
      String envName,
      List<String> errors
  ) {
    boolean enabled = Boolean.parseBoolean(environment.getProperty(propertyKey, "false"));
    if (!enabled) {
      errors.add(envName + " must be true in production.");
    }
  }

  private static void requireSameSiteNone(Environment environment, List<String> errors) {
    String sameSite = property(environment, "app.auth.cookies.same-site");
    if (!"none".equalsIgnoreCase(sameSite)) {
      errors.add("AUTH_COOKIE_SAME_SITE must be None for the cross-origin Vercel/Render deployment.");
    }
  }

  private static void requireMailIfEnabled(Environment environment, List<String> errors) {
    boolean mailEnabled = Boolean.parseBoolean(environment.getProperty("app.mail.enabled", "false"));
    if (mailEnabled) {
      requireSecret(environment, "app.mail.api-key", "RESEND_API_KEY", errors);
    }
  }

  private static void requireSecret(
      Environment environment,
      String propertyKey,
      String envName,
      List<String> errors
  ) {
    String value = property(environment, propertyKey);
    if (isPlaceholder(value)) {
      errors.add(envName + " must be set to a real production value.");
    }
  }

  private static void requireRawSecret(
      Environment environment,
      String envName,
      List<String> errors
  ) {
    String value = environment.getProperty(envName, "").trim();
    if (isPlaceholder(value)) {
      errors.add(envName + " must be set to a provider-specific webhook signing secret.");
    }
  }

  private static String property(Environment environment, String key) {
    return environment.getProperty(key, "").trim();
  }

  private static boolean isPlaceholder(String value) {
    if (!hasText(value)) {
      return true;
    }
    String normalized = value.trim().toLowerCase(Locale.ROOT);
    return normalized.equals("replace-me")
        || normalized.contains("replace-with")
        || normalized.contains("dev-secret")
        || normalized.contains("your-project")
        || normalized.contains("example");
  }

  private static boolean containsLocalhost(String value) {
    String normalized = value.toLowerCase(Locale.ROOT);
    return normalized.contains("localhost")
        || normalized.contains("127.0.0.1")
        || normalized.contains("0.0.0.0")
        || normalized.contains("[::1]");
  }

  private static boolean hasText(String value) {
    return value != null && !value.trim().isEmpty();
  }
}
