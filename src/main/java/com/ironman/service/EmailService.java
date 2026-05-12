package com.ironman.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:noreply@ironman-laundry.com}")
    private String fromAddress;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    // Runs on a background thread — never blocks the HTTP response
    @Async
    public void send(String to, String subject, String body) {
        if (!mailEnabled) {
            log.info("[EMAIL SKIPPED] To: {} | Subject: {}", to, subject);
            return;
        }
        try {
            var msg = new SimpleMailMessage();
            msg.setFrom(fromAddress);
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(body);
            mailSender.send(msg);
            log.info("Email sent → {} — {}", to, subject);
        } catch (Exception ex) {
            log.error("Email failed → {}: {}", to, ex.getMessage());
        }
    }

    // ── Templates ──────────────────────────────────────────────────────────────

    public void sendOrderPlaced(String to, String name, String orderNo, String amount) {
        send(to, "Order Placed — " + orderNo, """
        Dear %s,

        Your order has been placed successfully.

        Order : %s
        Total : BDT %s

        We will confirm it shortly. Track your order in the app.

        — Ironman Laundry
        """.formatted(name, orderNo, amount));
    }

    public void sendOrderConfirmed(String to, String name, String orderNo) {
        send(to, "Order Confirmed — " + orderNo, """
        Dear %s,

        Your order %s has been confirmed! A pickup agent will be assigned soon.

        — Ironman Laundry
        """.formatted(name, orderNo));
    }

    public void sendPickupAssigned(String to, String name, String orderNo, String agent) {
        send(to, "Pickup Assigned — " + orderNo, """
        Dear %s,

        %s will come to collect your clothes for order %s.
        Please keep your items ready.

        — Ironman Laundry
        """.formatted(name, agent, orderNo));
    }

    public void sendOrderStatusUpdate(String to, String name, String orderNo,
                                      String statusLabel, String detail) {
        send(to, statusLabel + " — " + orderNo, """
        Dear %s,

        Your order %s has been updated.

        Status : %s
        Detail : %s

        Track in the app for live updates.

        — Ironman Laundry
        """.formatted(name, orderNo, statusLabel, detail));
    }

    public void sendDeliveryAssigned(String to, String name, String orderNo, String agent) {
        send(to, "Out for Delivery — " + orderNo, """
        Dear %s,

        Your order %s is ready! %s will deliver it to you soon.

        — Ironman Laundry
        """.formatted(name, orderNo, agent));
    }

    public void sendOrderDelivered(String to, String name, String orderNo) {
        send(to, "Delivered — " + orderNo, """
        Dear %s,

        Your order %s has been delivered. Hope you love the results!
        Please rate our service in the app.

        — Ironman Laundry
        """.formatted(name, orderNo));
    }

    public void sendStaffAssignment(String to, String name, String task,
                                    String orderNo, String notes) {
        String noteLine = (notes != null && !notes.isBlank()) ? "\nNotes : " + notes : "";
        send(to, "New Task — " + task + " for " + orderNo, """
        Dear %s,

        You have a new assignment.

        Task  : %s
        Order : %s%s

        Open the app to accept and start.

        — Ironman Laundry
        """.formatted(name, task, orderNo, noteLine));
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
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body);
            if (attachment != null && filename != null) {
                helper.addAttachment(filename, new ByteArrayResource(attachment), contentType);
            }
            mailSender.send(message);
            log.info("Email with attachment sent → {} — {} ({})", to, subject, filename);
        } catch (Exception ex) {
            log.error("Email with attachment failed → {}: {}", to, ex.getMessage());
        }
    }

    public void sendAdminNewOrder(String to, String customer, String orderNo, String amount) {
        send(to, "[ADMIN] New Order — " + orderNo, """
        New order requires confirmation.

        Customer : %s
        Order    : %s
        Amount   : BDT %s

        Login to the admin panel to confirm.

        — Ironman Laundry System
        """.formatted(customer, orderNo, amount));
    }
}