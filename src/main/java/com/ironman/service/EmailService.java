package com.ironman.service;

import com.ironman.config.BrandingProperties;
import jakarta.mail.internet.MimeMessage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Sends transactional emails. Every message goes out as a multipart/alternative
 * MIME with a themed HTML body (logo embedded via CID) and a plaintext fallback
 * derived from the HTML — so it looks right in Gmail/Outlook and stays readable
 * in plaintext-only clients.
 *
 * Disabled mode ({@code app.mail.enabled=false}) just logs and returns; useful
 * in local dev.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

  private final JavaMailSender mailSender;
  private final EmailTemplateService templates;
  private final BrandingProperties branding;

  @Value("${app.mail.from:noreply@ironman-laundry.com}")
  private String fromAddress;

  @Value("${app.mail.enabled:false}")
  private boolean mailEnabled;

  // ─── Low-level send ──────────────────────────────────────────────────────

  /**
   * Generic notification email — used by NotificationService. Wraps the plain
   * body in the themed shell so all in-app notifications also arrive as
   * properly designed emails.
   */
  @Async
  public void send(String to, String subject, String body) {
    String html = templates.genericNotification(subject, body);
    sendHtml(to, subject, html);
  }

  /** Sends a pre-rendered HTML email with the logo embedded as inline CID. */
  @Async
  public void sendHtml(String to, String subject, String html) {
    if (!mailEnabled) {
      log.info("[EMAIL SKIPPED] To: {} | Subject: {}", to, subject);
      return;
    }
    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
      helper.setFrom(fromAddress, branding.getName());
      helper.setTo(to);
      helper.setSubject(subject);

      // HTML body + plaintext alternative.
      helper.setText(templates.toPlainText(html), html);

      attachLogoIfPresent(helper);

      mailSender.send(message);
      log.info("Email sent → {} — {}", to, subject);
    } catch (Exception ex) {
      log.error("Email failed → {}: {}", to, ex.getMessage());
    }
  }

  /** Attaches a PDF/binary while keeping the themed HTML body. */
  @Async
  public void sendWithAttachment(String to, String subject, String body,
                                 String filename, byte[] attachment,
                                 String contentType) {
    if (!mailEnabled) {
      log.info("[EMAIL SKIPPED] To: {} | Subject: {} | Attachment: {} ({} bytes)",
          to, subject, filename, attachment == null ? 0 : attachment.length);
      return;
    }
    try {
      String html = templates.genericNotification(subject, body);
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
      helper.setFrom(fromAddress, branding.getName());
      helper.setTo(to);
      helper.setSubject(subject);
      helper.setText(templates.toPlainText(html), html);
      attachLogoIfPresent(helper);
      if (attachment != null && filename != null) {
        helper.addAttachment(filename, new ByteArrayResource(attachment), contentType);
      }
      mailSender.send(message);
      log.info("Email with attachment sent → {} — {} ({})", to, subject, filename);
    } catch (Exception ex) {
      log.error("Email with attachment failed → {}: {}", to, ex.getMessage());
    }
  }

  private void attachLogoIfPresent(MimeMessageHelper helper) throws Exception {
    if (!templates.hasLogo()) return;
    Path logoPath = Paths.get(branding.getLogoPath());
    if (!Files.exists(logoPath) || !Files.isReadable(logoPath)) {
      log.debug("Brand logo not readable at {} — sending without inline image", logoPath);
      return;
    }
    helper.addInline(templates.logoContentId(), new FileSystemResource(logoPath));
  }

  // ─── Templates (every one a themed HTML email) ───────────────────────────

  public void sendWelcome(String to, String name) {
    String inner = ""
        + "<p style=\"margin:0;font:400 15px/1.6 'Helvetica Neue',Arial,sans-serif;color:#374151;\">"
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
        + "If you didn’t try to sign up or sign in, you can safely ignore this email.</p>";
    sendHtml(to, "Verify your email — code " + code,
        templates.wrap("Confirm your email", intro, inner));
  }

  public void sendPasswordReset(String to, String name, String code, int expiryMinutes) {
    String intro = "Hi " + escape(name) + ", we received a request to reset your password. "
        + "Use the code below to choose a new one. It expires in " + expiryMinutes + " minutes.";
    String inner = templates.codeBox(code, "Enter this code in the password-reset screen")
        + "<p style=\"margin:14px 0 0 0;font:400 13px/1.6 'Helvetica Neue',Arial,sans-serif;color:#6b7280;\">"
        + "If you didn’t request a password reset, ignore this email — your password stays the same.</p>";
    sendHtml(to, "Reset your password — code " + code,
        templates.wrap("Reset your password", intro, inner));
  }

  public void sendPasswordChanged(String to, String name) {
    String inner = "<p style=\"margin:0;font:400 15px/1.6 'Helvetica Neue',Arial,sans-serif;color:#374151;\">"
        + "Hi " + escape(name) + ", your password was just updated. If this wasn’t you, "
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
                + "We’ll confirm and assign a pickup agent shortly.", inner));
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
