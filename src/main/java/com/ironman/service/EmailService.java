package com.ironman.service;

import com.ironman.config.BrandingProperties;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Sends transactional emails via the Resend HTTP API (https://resend.com).
 * Uses HTTPS on port 443 — not SMTP — so it works on Render and similar
 * platforms that block outbound port 587/465.
 *
 * Disabled mode (app.mail.enabled=false) just logs and returns; useful in
 * local dev where RESEND_API_KEY is not configured.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

  private static final String RESEND_API = "https://api.resend.com/emails";

  private final EmailTemplateService templates;
  private final BrandingProperties branding;
  private final RestTemplate restTemplate;

  @Value("${app.mail.from:noreply@ironman-laundry.com}")
  private String fromAddress;

  @Value("${app.mail.api-key:}")
  private String apiKey;

  @Value("${app.mail.enabled:false}")
  private boolean mailEnabled;

  // ─── Low-level send ──────────────────────────────────────────────────────

  @Async
  public void send(String to, String subject, String body) {
    sendHtml(to, subject, templates.genericNotification(subject, body));
  }

  @Async
  public void sendHtml(String to, String subject, String html) {
    if (!mailEnabled) {
      log.info("[EMAIL SKIPPED] To: {} | Subject: {}", to, subject);
      return;
    }
    if (apiKey == null || apiKey.isBlank()) {
      log.warn("[EMAIL SKIPPED] RESEND_API_KEY not set — To: {} | Subject: {}", to, subject);
      return;
    }
    try {
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      headers.setBearerAuth(apiKey);

      Map<String, Object> body = Map.of(
          "from", branding.getName() + " <" + fromAddress + ">",
          "to", List.of(to),
          "subject", subject,
          "html", html,
          "text", templates.toPlainText(html)
      );

      restTemplate.postForObject(RESEND_API, new HttpEntity<>(body, headers), Map.class);
      log.info("Email sent → {} — {}", to, subject);
    } catch (Exception ex) {
      log.error("Email failed → {}: {}", to, ex.getMessage());
    }
  }

  @Async
  public void sendWithAttachment(String to, String subject, String body,
                                 String filename, byte[] attachment,
                                 String contentType) {
    if (!mailEnabled) {
      log.info("[EMAIL SKIPPED] To: {} | Subject: {} | Attachment: {} ({} bytes)",
          to, subject, filename, attachment == null ? 0 : attachment.length);
      return;
    }
    // Resend supports attachments as base64 in the JSON payload
    if (apiKey == null || apiKey.isBlank()) {
      log.warn("[EMAIL SKIPPED] RESEND_API_KEY not set — attachment email dropped");
      return;
    }
    try {
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      headers.setBearerAuth(apiKey);

      String html = templates.genericNotification(subject, body);

      var payload = new java.util.HashMap<String, Object>();
      payload.put("from", branding.getName() + " <" + fromAddress + ">");
      payload.put("to", List.of(to));
      payload.put("subject", subject);
      payload.put("html", html);
      payload.put("text", templates.toPlainText(html));

      if (attachment != null && filename != null) {
        String encoded = java.util.Base64.getEncoder().encodeToString(attachment);
        payload.put("attachments", List.of(Map.of(
            "filename", filename,
            "content", encoded
        )));
      }

      restTemplate.postForObject(RESEND_API, new HttpEntity<>(payload, headers), Map.class);
      log.info("Email with attachment sent → {} — {} ({})", to, subject, filename);
    } catch (Exception ex) {
      log.error("Email with attachment failed → {}: {}", to, ex.getMessage());
    }
  }

  // ─── Templates ───────────────────────────────────────────────────────────

  public void sendWelcome(String to, String name) {
    String inner = "<p style=\"margin:0;font:400 15px/1.6 'Helvetica Neue',Arial,sans-serif;color:#374151;\">"
        + "Welcome aboard! Your account is ready. You can now place laundry orders, "
        + "schedule pickups, and track everything live from the app."
        + "</p>"
        + templates.button("Open the app", branding.getWebsite());
    sendHtml(to, "Welcome to " + branding.getName(),
        templates.wrap("Welcome, " + escape(name) + " 👋", "", inner));
  }

  public void sendOtpVerification(String to, String name, String code, int expiryMinutes) {
    String intro = "Hi " + escape(name) + ", use the verification code below to confirm your email "
        + "address. The code expires in " + expiryMinutes + " minutes.";
    String inner = templates.codeBox(code, "This code expires in " + expiryMinutes + " minutes")
        + "<p style=\"margin:14px 0 0 0;font:400 13px/1.6 'Helvetica Neue',Arial,sans-serif;color:#6b7280;\">"
        + "If you didn't try to sign up or sign in, you can safely ignore this email.</p>";
    sendHtml(to, "Verify your email — code " + code,
        templates.wrap("Confirm your email", intro, inner));
  }

  public void sendPasswordReset(String to, String name, String code, int expiryMinutes) {
    String intro = "Hi " + escape(name) + ", we received a request to reset your password. "
        + "Use the code below to choose a new one. It expires in " + expiryMinutes + " minutes.";
    String inner = templates.codeBox(code, "Enter this code in the password-reset screen")
        + "<p style=\"margin:14px 0 0 0;font:400 13px/1.6 'Helvetica Neue',Arial,sans-serif;color:#6b7280;\">"
        + "If you didn't request a password reset, ignore this email — your password stays the same.</p>";
    sendHtml(to, "Reset your password — code " + code,
        templates.wrap("Reset your password", intro, inner));
  }

  public void sendPasswordChanged(String to, String name) {
    String inner = "<p style=\"margin:0;font:400 15px/1.6 'Helvetica Neue',Arial,sans-serif;color:#374151;\">"
        + "Hi " + escape(name) + ", your password was just updated. If this wasn't you, "
        + "contact support immediately.</p>";
    sendHtml(to, "Your password was changed",
        templates.wrap("Password updated", "", inner));
  }

  public void sendOrderPlaced(String to, String name, String orderNo, String amount) {
    Map<String, String> rows = EmailTemplateService.rows();
    rows.put("Order", orderNo);
    rows.put("Amount", "BDT " + amount);
    rows.put("Status", "Awaiting confirmation");
    String inner = templates.detailsTable(rows)
        + templates.button("Track this order", linkToOrder(orderNo));
    sendHtml(to, "Order placed — " + orderNo,
        templates.wrap("We received your order",
            "Dear " + escape(name) + ", thanks for placing your order with us. "
                + "We'll confirm and assign a pickup agent shortly.", inner));
  }

  public void sendOrderConfirmed(String to, String name, String orderNo) {
    Map<String, String> rows = EmailTemplateService.rows();
    rows.put("Order", orderNo);
    rows.put("Status", "Confirmed");
    String inner = templates.detailsTable(rows)
        + templates.button("View order details", linkToOrder(orderNo));
    sendHtml(to, "Order confirmed — " + orderNo,
        templates.wrap("Your order is confirmed",
            "Hi " + escape(name) + ", your order has been confirmed. A pickup agent will be assigned soon.",
            inner));
  }

  public void sendPickupAssigned(String to, String name, String orderNo, String agent) {
    Map<String, String> rows = EmailTemplateService.rows();
    rows.put("Order", orderNo);
    rows.put("Pickup agent", agent);
    String inner = templates.detailsTable(rows)
        + "<p style=\"margin:14px 0 0 0;font:400 14px/1.6 'Helvetica Neue',Arial,sans-serif;color:#374151;\">"
        + "Please keep your items ready and a contact reachable at the pickup window.</p>";
    sendHtml(to, "Pickup assigned — " + orderNo,
        templates.wrap("Pickup is on the way",
            "Hi " + escape(name) + ", a pickup agent has been assigned to your order.", inner));
  }

  public void sendOrderStatusUpdate(String to, String name, String orderNo,
                                    String statusLabel, String detail) {
    Map<String, String> rows = EmailTemplateService.rows();
    rows.put("Order", orderNo);
    rows.put("Status", statusLabel);
    if (detail != null && !detail.isBlank()) rows.put("Detail", detail);
    String inner = templates.detailsTable(rows)
        + templates.button("Track live", linkToOrder(orderNo));
    sendHtml(to, statusLabel + " — " + orderNo,
        templates.wrap("Order update", "Hi " + escape(name) + ", your order status has changed.", inner));
  }

  public void sendDeliveryAssigned(String to, String name, String orderNo, String agent) {
    Map<String, String> rows = EmailTemplateService.rows();
    rows.put("Order", orderNo);
    rows.put("Delivery agent", agent);
    String inner = templates.detailsTable(rows)
        + templates.button("Track delivery", linkToOrder(orderNo));
    sendHtml(to, "Out for delivery — " + orderNo,
        templates.wrap("Your order is on its way",
            "Hi " + escape(name) + ", your laundry is ready. "
                + escape(agent) + " will deliver it soon.", inner));
  }

  public void sendOrderDelivered(String to, String name, String orderNo) {
    Map<String, String> rows = EmailTemplateService.rows();
    rows.put("Order", orderNo);
    rows.put("Status", "Delivered");
    String inner = templates.detailsTable(rows)
        + "<p style=\"margin:14px 0 0 0;font:400 15px/1.6 'Helvetica Neue',Arial,sans-serif;color:#374151;\">"
        + "We hope you love the results. A quick rating helps us keep our standards high.</p>"
        + templates.button("Rate your order", linkToOrder(orderNo));
    sendHtml(to, "Delivered — " + orderNo,
        templates.wrap("Delivered. Thank you!",
            "Hi " + escape(name) + ", your order has been delivered.", inner));
  }

  public void sendStaffAssignment(String to, String name, String task,
                                  String orderNo, String notes) {
    Map<String, String> rows = EmailTemplateService.rows();
    rows.put("Task", task);
    rows.put("Order", orderNo);
    if (notes != null && !notes.isBlank()) rows.put("Notes", notes);
    String inner = templates.detailsTable(rows)
        + templates.button("Open the app", branding.getWebsite());
    sendHtml(to, "New task — " + task + " for " + orderNo,
        templates.wrap("You have a new assignment",
            "Hi " + escape(name) + ", a new task has been assigned to you.", inner));
  }

  public void sendAdminNewOrder(String to, String customer, String orderNo, String amount) {
    Map<String, String> rows = EmailTemplateService.rows();
    rows.put("Customer", customer);
    rows.put("Order", orderNo);
    rows.put("Amount", "BDT " + amount);
    String inner = templates.detailsTable(rows)
        + templates.button("Open admin panel", branding.getWebsite());
    sendHtml(to, "[Admin] New order — " + orderNo,
        templates.wrap("A new order needs confirmation",
            "An order was just placed and is waiting in the admin queue.", inner));
  }

  // ─── helpers ─────────────────────────────────────────────────────────────

  private String linkToOrder(String orderNo) {
    String base = branding.getWebsite();
    if (base == null || base.isBlank()) return "";
    String trimmed = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    return trimmed + "/track?order=" + orderNo;
  }

  private static String escape(String value) {
    if (value == null) return "";
    return value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;");
  }
}
