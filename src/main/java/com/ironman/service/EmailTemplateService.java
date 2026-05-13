package com.ironman.service;

import com.ironman.config.BrandingProperties;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Builds branded HTML email bodies that all share the same letterhead
 * (logo, theme color, footer). Email clients are notoriously hostile to
 * modern CSS, so this uses tables, inline styles, and hex colors only.
 *
 * The logo is referenced via {@code cid:ironman-logo} — {@code EmailService}
 * is responsible for attaching the actual bytes when it builds the MIME
 * message.
 */
@Service
@RequiredArgsConstructor
public class EmailTemplateService {

  private static final Pattern HTML_TAG = Pattern.compile("<[^>]+>");

  private final BrandingProperties branding;

  public String logoContentId() {
    return "ironman-logo";
  }

  /** True when a logo file is configured and present. */
  public boolean hasLogo() {
    return branding.getLogoPath() != null && !branding.getLogoPath().isBlank();
  }

  /** Plain-text fallback derived from an HTML body — for the multipart alternative. */
  public String toPlainText(String html) {
    return HTML_TAG.matcher(html)
        .replaceAll("")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replaceAll("(?m)^[ \\t]+", "")
        .replaceAll("\\n{3,}", "\n\n")
        .trim();
  }

  // ─── Reusable building blocks ────────────────────────────────────────────

  /** Wraps the inner HTML body in a full themed email shell. */
  public String wrap(String heading, String introHtml, String innerHtml) {
    String accent = safeColor(branding.getThemeColor());
    String name = escape(branding.getName());
    String tagline = escape(branding.getTagline());
    String website = branding.getWebsite() == null ? "" : escape(branding.getWebsite());
    String supportEmail = branding.getContactEmail() == null ? "" : escape(branding.getContactEmail());
    String supportPhone = branding.getContactPhone() == null ? "" : escape(branding.getContactPhone());
    String address = branding.getCompanyAddress() == null ? "" : escape(branding.getCompanyAddress());

    String logoBlock = hasLogo()
        ? "<img src=\"cid:" + logoContentId() + "\" alt=\"" + name
            + "\" width=\"56\" height=\"56\" style=\"display:block;border-radius:8px;background:#ffffff;padding:6px;\" />"
        : "<div style=\"font:700 22px/1 'Helvetica Neue',Arial,sans-serif;color:#ffffff;letter-spacing:1px;\">"
            + name.toUpperCase() + "</div>";

    return ""
        + "<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"UTF-8\"/>"
        + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"/>"
        + "<title>" + escape(heading) + "</title></head>"
        + "<body style=\"margin:0;padding:0;background:#eef0f8;font-family:'Helvetica Neue',Arial,sans-serif;color:#1B2454;\">"
        + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"background:#eef0f8;\">"
        + "<tr><td align=\"center\" style=\"padding:24px 12px;\">"
        + "<table role=\"presentation\" width=\"600\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" "
        + "style=\"max-width:600px;width:100%;background:#ffffff;border-radius:14px;overflow:hidden;"
        + "box-shadow:0 18px 45px rgba(27,36,84,0.10);\">"

        // ── Header bar ────────────────────────────────────────────────────
        + "<tr><td style=\"background:" + accent + ";padding:24px 28px;\">"
        + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"><tr>"
        + "<td valign=\"middle\" style=\"width:64px;\">" + logoBlock + "</td>"
        + "<td valign=\"middle\" style=\"padding-left:14px;color:#ffffff;\">"
        + "<div style=\"font:700 20px/1.2 'Helvetica Neue',Arial,sans-serif;\">" + name + "</div>"
        + "<div style=\"font:400 12px/1.4 'Helvetica Neue',Arial,sans-serif;opacity:0.9;margin-top:2px;\">" + tagline + "</div>"
        + "</td></tr></table>"
        + "</td></tr>"

        // ── Heading + intro ───────────────────────────────────────────────
        + "<tr><td style=\"padding:32px 32px 8px 32px;\">"
        + "<h1 style=\"margin:0;font:700 24px/1.3 'Helvetica Neue',Arial,sans-serif;color:#1B2454;\">"
        + escape(heading) + "</h1>"
        + (introHtml == null || introHtml.isBlank() ? ""
            : "<p style=\"margin:14px 0 0 0;font:400 15px/1.6 'Helvetica Neue',Arial,sans-serif;color:#374151;\">"
                + introHtml + "</p>")
        + "</td></tr>"

        // ── Body ──────────────────────────────────────────────────────────
        + "<tr><td style=\"padding:18px 32px 28px 32px;\">"
        + (innerHtml == null ? "" : innerHtml)
        + "</td></tr>"

        // ── Divider + signature ───────────────────────────────────────────
        + "<tr><td style=\"padding:0 32px;\">"
        + "<div style=\"border-top:1px solid #eef0f8;\"></div>"
        + "<p style=\"margin:18px 0 4px 0;font:400 14px/1.6 'Helvetica Neue',Arial,sans-serif;color:#374151;\">"
        + "Thanks for choosing " + name + ".</p>"
        + "<p style=\"margin:0 0 28px 0;font:600 14px/1.6 'Helvetica Neue',Arial,sans-serif;color:#1B2454;\">"
        + "— The " + name + " team</p>"
        + "</td></tr>"

        // ── Footer ────────────────────────────────────────────────────────
        + "<tr><td style=\"background:#1B2454;padding:18px 32px;color:#ffffff;\">"
        + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"><tr>"
        + "<td style=\"font:400 12px/1.6 'Helvetica Neue',Arial,sans-serif;color:#d1d6ec;\">"
        + (address.isEmpty() ? "" : "<div>" + address + "</div>")
        + (supportPhone.isEmpty() ? "" : "<div>" + supportPhone + "</div>")
        + (supportEmail.isEmpty() ? "" : "<div><a href=\"mailto:" + supportEmail
            + "\" style=\"color:#e8c96e;text-decoration:none;\">" + supportEmail + "</a></div>")
        + (website.isEmpty() ? "" : "<div><a href=\"" + website
            + "\" style=\"color:#e8c96e;text-decoration:none;\">" + website + "</a></div>")
        + "</td></tr></table></td></tr>"

        + "</table>"
        + "<p style=\"margin:14px 0 0 0;font:400 11px/1.6 'Helvetica Neue',Arial,sans-serif;color:#9ca3af;\">"
        + "This is an automated message. If you didn’t request it, you can safely ignore this email.</p>"
        + "</td></tr></table></body></html>";
  }

  /** Renders a label/value table the way the receipt does, but for emails. */
  public String detailsTable(Map<String, String> rows) {
    if (rows == null || rows.isEmpty()) return "";
    StringBuilder sb = new StringBuilder();
    sb.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" ")
      .append("style=\"background:#eef0f8;border-radius:10px;padding:6px 10px;\">");
    for (Map.Entry<String, String> e : rows.entrySet()) {
      sb.append("<tr>")
        .append("<td style=\"padding:8px 10px;font:600 12px/1.4 'Helvetica Neue',Arial,sans-serif;")
        .append("color:#1B2454;text-transform:uppercase;letter-spacing:0.4px;width:40%;\">")
        .append(escape(e.getKey())).append("</td>")
        .append("<td style=\"padding:8px 10px;font:400 14px/1.4 'Helvetica Neue',Arial,sans-serif;color:#374151;\">")
        .append(e.getValue() == null ? "—" : escape(e.getValue())).append("</td>")
        .append("</tr>");
    }
    sb.append("</table>");
    return sb.toString();
  }

  /** A theme-colored CTA button. URL is optional; falls back to a static badge. */
  public String button(String label, String url) {
    String accent = safeColor(branding.getThemeColor());
    if (url == null || url.isBlank()) {
      return "<div style=\"margin:22px 0;\">"
          + "<span style=\"display:inline-block;background:" + accent + ";color:#ffffff;"
          + "padding:12px 22px;border-radius:10px;font:700 14px/1 'Helvetica Neue',Arial,sans-serif;\">"
          + escape(label) + "</span></div>";
    }
    return "<div style=\"margin:22px 0;\">"
        + "<a href=\"" + escape(url) + "\" "
        + "style=\"display:inline-block;background:" + accent + ";color:#ffffff;text-decoration:none;"
        + "padding:12px 22px;border-radius:10px;font:700 14px/1 'Helvetica Neue',Arial,sans-serif;\">"
        + escape(label) + "</a></div>";
  }

  /** Highlight panel for an OTP code or reference number. */
  public String codeBox(String code, String caption) {
    String accent = safeColor(branding.getThemeColor());
    return "<div style=\"margin:22px 0;text-align:center;\">"
        + "<div style=\"display:inline-block;border:2px dashed " + accent + ";border-radius:12px;"
        + "padding:18px 32px;\">"
        + "<div style=\"font:700 32px/1 'Helvetica Neue',Arial,sans-serif;color:" + accent
        + ";letter-spacing:8px;\">" + escape(code) + "</div>"
        + (caption == null || caption.isBlank() ? ""
            : "<div style=\"margin-top:8px;font:400 12px/1.4 'Helvetica Neue',Arial,sans-serif;color:#6b7280;\">"
                + escape(caption) + "</div>")
        + "</div></div>";
  }

  /** Convenience: wraps a plain notification body in the email shell. */
  public String genericNotification(String heading, String body) {
    String inner = "<p style=\"margin:0;font:400 15px/1.6 'Helvetica Neue',Arial,sans-serif;color:#374151;white-space:pre-line;\">"
        + escape(body == null ? "" : body) + "</p>";
    return wrap(heading, "", inner);
  }

  public static Map<String, String> rows() {
    return new LinkedHashMap<>();
  }

  // ─── helpers ─────────────────────────────────────────────────────────────

  private static String escape(String value) {
    if (value == null) return "";
    StringBuilder sb = new StringBuilder(value.length());
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      switch (c) {
        case '&' -> sb.append("&amp;");
        case '<' -> sb.append("&lt;");
        case '>' -> sb.append("&gt;");
        case '"' -> sb.append("&quot;");
        case '\'' -> sb.append("&#39;");
        default -> sb.append(c);
      }
    }
    return sb.toString();
  }

  private static String safeColor(String hex) {
    if (hex == null) return "#F5A623";
    String trimmed = hex.trim();
    return trimmed.matches("^#[0-9A-Fa-f]{3,8}$") ? trimmed : "#F5A623";
  }
}
