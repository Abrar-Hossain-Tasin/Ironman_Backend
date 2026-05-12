package com.ironman.service;

import com.ironman.config.BrandingProperties;
import com.ironman.model.Address;
import com.ironman.model.CodConfirmationStatus;
import com.ironman.model.LaundryOrder;
import com.ironman.model.OrderItem;
import com.ironman.model.OrderReceipt;
import com.ironman.model.PaymentMethod;
import com.ironman.model.PaymentStatus;
import com.ironman.model.User;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfGState;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReceiptPdfService {

  private static final ZoneId DHAKA = ZoneId.of("Asia/Dhaka");
  private static final DateTimeFormatter DATE_TIME =
      DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
  private static final DateTimeFormatter DATE_ONLY =
      DateTimeFormatter.ofPattern("dd MMM yyyy");

  private final BrandingProperties branding;

  public byte[] render(LaundryOrder order, OrderReceipt receipt,
                       List<OrderItem> items, User deliveryMan) {
    Color accent = parseColor(branding.getThemeColor(), new Color(0xF5, 0xA6, 0x23));
    boolean paid = isPaid(order);

    Document document = new Document(PageSize.A4, 36, 36, 110, 70);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    PdfWriter writer = PdfWriter.getInstance(document, out);

    writer.setPageEvent(new BrandedPageEvent(accent, paid));
    document.open();

    addTitleBlock(document, receipt);
    addCustomerAndOrderBlock(document, order);
    addDeliveryManBlock(document, deliveryMan);
    addPaymentTable(document, order, items, accent);
    addThankYouBlock(document);

    document.close();
    return out.toByteArray();
  }

  // ── Sections ──────────────────────────────────────────────────────────────

  private void addTitleBlock(Document document, OrderReceipt receipt) {
    Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.DARK_GRAY);
    Font metaFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);

    Paragraph title = new Paragraph("Service Charge Receipt", titleFont);
    title.setSpacingBefore(4f);
    title.setSpacingAfter(2f);
    document.add(title);

    String generated = receipt.getGeneratedAt()
        .atZone(DHAKA).format(DATE_TIME);
    Paragraph meta = new Paragraph(
        "Voucher No: " + receipt.getReceiptNumber() + "    |    Issued: " + generated,
        metaFont);
    meta.setSpacingAfter(14f);
    document.add(meta);
  }

  private void addCustomerAndOrderBlock(Document document, LaundryOrder order) {
    User customer = order.getCustomer();
    PdfPTable table = twoColumn();

    sectionHeader(table, "Customer & Order");
    row(table, "Customer Name", safe(customer.getFullName()));
    row(table, "Phone", safe(customer.getPhone()));
    row(table, "Email", safe(customer.getEmail()));
    row(table, "Order ID", safe(order.getOrderNumber()));
    row(table, "Order Date",
        order.getCreatedAt().atZone(DHAKA).format(DATE_ONLY));
    row(table, "Delivery Address", formatAddress(order.getDeliveryAddress()));

    document.add(table);
  }

  private void addDeliveryManBlock(Document document, User deliveryMan) {
    PdfPTable table = twoColumn();
    sectionHeader(table, "Delivery Agent");
    if (deliveryMan == null) {
      row(table, "Status", "Not yet assigned");
    } else {
      row(table, "Name", safe(deliveryMan.getFullName()));
      row(table, "Phone", safe(deliveryMan.getPhone()));
      row(table, "Agent ID", deliveryMan.getId().toString());
    }
    document.add(table);
  }

  private void addPaymentTable(Document document, LaundryOrder order,
                               List<OrderItem> items, Color accent) {
    Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
    Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);
    Font totalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.BLACK);

    PdfPTable table = new PdfPTable(new float[]{4.5f, 1.5f, 2f, 2f});
    table.setWidthPercentage(100);
    table.setSpacingBefore(14f);
    table.setSpacingAfter(10f);

    addHeaderCell(table, "Description", headerFont, accent);
    addHeaderCell(table, "Qty", headerFont, accent);
    addHeaderCell(table, "Unit (BDT)", headerFont, accent);
    addHeaderCell(table, "Amount (BDT)", headerFont, accent);

    BigDecimal subtotal = BigDecimal.ZERO;
    for (OrderItem item : items) {
      String desc = item.getServiceCategory().getName()
          + " — " + item.getClothingType().getName();
      addBodyCell(table, desc, cellFont, Element.ALIGN_LEFT);
      addBodyCell(table, String.valueOf(item.getQuantity()), cellFont, Element.ALIGN_CENTER);
      addBodyCell(table, format(item.getUnitPrice()), cellFont, Element.ALIGN_RIGHT);
      addBodyCell(table, format(item.getSubtotal()), cellFont, Element.ALIGN_RIGHT);
      subtotal = subtotal.add(item.getSubtotal());
    }

    BigDecimal total = nullSafe(order.getTotalAmount());
    BigDecimal deliveryCharge = total.subtract(subtotal).max(BigDecimal.ZERO);
    BigDecimal discount = BigDecimal.ZERO;
    BigDecimal tax = BigDecimal.ZERO;

    summaryRow(table, "Items subtotal", format(subtotal), cellFont);
    summaryRow(table, "Delivery / service charge", format(deliveryCharge), cellFont);
    summaryRow(table, "Discount", "-" + format(discount), cellFont);
    summaryRow(table, "Tax", format(tax), cellFont);
    totalRow(table, "TOTAL PAID (BDT)", format(total), totalFont, accent);

    document.add(table);

    PdfPTable metaTable = twoColumn();
    row(metaTable, "Payment Method", paymentMethodLabel(order.getPaymentMethod()));
    row(metaTable, "Payment Status", paymentStatusLabel(order));
    document.add(metaTable);
  }

  private void addThankYouBlock(Document document) {
    Font thanksFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 11, Color.DARK_GRAY);
    Font fine = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);

    Paragraph thanks = new Paragraph(
        "Thank you for choosing " + branding.getName() + ".", thanksFont);
    thanks.setSpacingBefore(16f);
    thanks.setAlignment(Element.ALIGN_CENTER);
    document.add(thanks);

    Paragraph terms = new Paragraph(
        "This is a system-generated receipt and is valid without a signature. "
            + "For queries please contact us at "
            + safe(branding.getContactEmail()) + ".",
        fine);
    terms.setAlignment(Element.ALIGN_CENTER);
    terms.setSpacingBefore(4f);
    document.add(terms);
  }

  // ── Page event: header bar, footer bar, logo, watermark ───────────────────

  private class BrandedPageEvent extends PdfPageEventHelper {
    private final Color accent;
    private final boolean paid;

    BrandedPageEvent(Color accent, boolean paid) {
      this.accent = accent;
      this.paid = paid;
    }

    @Override
    public void onEndPage(PdfWriter writer, Document document) {
      PdfContentByte cb = writer.getDirectContent();
      Rectangle page = document.getPageSize();

      cb.saveState();
      cb.setColorFill(accent);
      cb.rectangle(0, page.getTop() - 70, page.getWidth(), 70);
      cb.fill();
      cb.restoreState();

      cb.saveState();
      cb.setColorFill(accent);
      cb.rectangle(0, 0, page.getWidth(), 32);
      cb.fill();
      cb.restoreState();

      Font brandFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.WHITE);
      Font taglineFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, Color.WHITE);
      Phrase brand = new Phrase(safe(branding.getName()), brandFont);
      com.lowagie.text.pdf.ColumnText.showTextAligned(
          cb, Element.ALIGN_LEFT, brand,
          document.leftMargin() + 60, page.getTop() - 35, 0);
      if (branding.getTagline() != null && !branding.getTagline().isBlank()) {
        com.lowagie.text.pdf.ColumnText.showTextAligned(
            cb, Element.ALIGN_LEFT, new Phrase(branding.getTagline(), taglineFont),
            document.leftMargin() + 60, page.getTop() - 50, 0);
      }

      drawLogo(cb, page, document);

      Font footerFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.WHITE);
      String footer = String.join(" • ",
          stripBlank(branding.getCompanyAddress()),
          stripBlank(branding.getContactPhone()),
          stripBlank(branding.getContactEmail()),
          stripBlank(branding.getWebsite()));
      com.lowagie.text.pdf.ColumnText.showTextAligned(
          cb, Element.ALIGN_CENTER, new Phrase(footer, footerFont),
          page.getWidth() / 2f, 12, 0);

      if (paid) {
        drawPaidWatermark(cb, page);
      }
    }

    private void drawLogo(PdfContentByte cb, Rectangle page, Document document) {
      String path = branding.getLogoPath();
      if (path == null || path.isBlank()) {
        return;
      }
      try {
        Path resolved = Paths.get(path);
        if (!Files.exists(resolved)) {
          log.debug("Receipt logo not found at {}", resolved.toAbsolutePath());
          return;
        }
        Image logo = Image.getInstance(resolved.toAbsolutePath().toString());
        logo.scaleToFit(48, 48);
        logo.setAbsolutePosition(
            document.leftMargin(), page.getTop() - 60);
        cb.addImage(logo);
      } catch (IOException ex) {
        log.warn("Failed to embed receipt logo: {}", ex.getMessage());
      }
    }

    private void drawPaidWatermark(PdfContentByte cb, Rectangle page) {
      cb.saveState();
      PdfGState gs = new PdfGState();
      gs.setFillOpacity(0.18f);
      cb.setGState(gs);
      Font watermarkFont = FontFactory.getFont(
          FontFactory.HELVETICA_BOLD, 120, new Color(0x2E, 0x8B, 0x57));
      Phrase watermark = new Phrase("PAID", watermarkFont);
      com.lowagie.text.pdf.ColumnText.showTextAligned(
          cb, Element.ALIGN_CENTER, watermark,
          page.getWidth() / 2f, page.getHeight() / 2f - 60, 30);
      cb.restoreState();
    }
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  private PdfPTable twoColumn() {
    PdfPTable table = new PdfPTable(new float[]{2f, 5f});
    table.setWidthPercentage(100);
    table.setSpacingBefore(6f);
    table.setSpacingAfter(2f);
    return table;
  }

  private void sectionHeader(PdfPTable table, String text) {
    Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.DARK_GRAY);
    PdfPCell cell = new PdfPCell(new Phrase(text, font));
    cell.setColspan(2);
    cell.setBorder(Rectangle.BOTTOM);
    cell.setBorderColor(new Color(0xCC, 0xCC, 0xCC));
    cell.setBorderWidthBottom(0.8f);
    cell.setPaddingTop(8f);
    cell.setPaddingBottom(4f);
    table.addCell(cell);
  }

  private void row(PdfPTable table, String label, String value) {
    Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.GRAY);
    Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);
    PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
    PdfPCell valueCell = new PdfPCell(new Phrase(value == null ? "—" : value, valueFont));
    labelCell.setBorder(Rectangle.NO_BORDER);
    valueCell.setBorder(Rectangle.NO_BORDER);
    labelCell.setPaddingTop(3f);
    valueCell.setPaddingTop(3f);
    table.addCell(labelCell);
    table.addCell(valueCell);
  }

  private void addHeaderCell(PdfPTable table, String text, Font font, Color accent) {
    PdfPCell cell = new PdfPCell(new Phrase(text, font));
    cell.setBackgroundColor(accent);
    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
    cell.setPadding(6f);
    cell.setBorderColor(accent);
    table.addCell(cell);
  }

  private void addBodyCell(PdfPTable table, String text, Font font, int align) {
    PdfPCell cell = new PdfPCell(new Phrase(text, font));
    cell.setHorizontalAlignment(align);
    cell.setPadding(5f);
    cell.setBorderColor(new Color(0xE0, 0xE0, 0xE0));
    table.addCell(cell);
  }

  private void summaryRow(PdfPTable table, String label, String value, Font font) {
    PdfPCell labelCell = new PdfPCell(new Phrase(label, font));
    labelCell.setColspan(3);
    labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
    labelCell.setPadding(5f);
    labelCell.setBorderColor(new Color(0xE0, 0xE0, 0xE0));
    PdfPCell valueCell = new PdfPCell(new Phrase(value, font));
    valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
    valueCell.setPadding(5f);
    valueCell.setBorderColor(new Color(0xE0, 0xE0, 0xE0));
    table.addCell(labelCell);
    table.addCell(valueCell);
  }

  private void totalRow(PdfPTable table, String label, String value, Font font,
                        Color accent) {
    PdfPCell labelCell = new PdfPCell(new Phrase(label, font));
    labelCell.setColspan(3);
    labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
    labelCell.setBackgroundColor(accent);
    labelCell.setPadding(7f);
    labelCell.setBorderColor(accent);
    Font white = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.WHITE);
    labelCell.setPhrase(new Phrase(label, white));
    PdfPCell valueCell = new PdfPCell(new Phrase(value, white));
    valueCell.setBackgroundColor(accent);
    valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
    valueCell.setPadding(7f);
    valueCell.setBorderColor(accent);
    table.addCell(labelCell);
    table.addCell(valueCell);
  }

  private String formatAddress(Address address) {
    if (address == null) {
      return "—";
    }
    StringBuilder sb = new StringBuilder(safe(address.getAddressLine1()));
    if (address.getAddressLine2() != null && !address.getAddressLine2().isBlank()) {
      sb.append(", ").append(address.getAddressLine2());
    }
    sb.append(", ").append(safe(address.getArea()));
    sb.append(", ").append(safe(address.getCity()));
    if (address.getPostalCode() != null && !address.getPostalCode().isBlank()) {
      sb.append(" — ").append(address.getPostalCode());
    }
    return sb.toString();
  }

  private boolean isPaid(LaundryOrder order) {
    return order.getCodConfirmationStatus() == CodConfirmationStatus.delivery_confirmed
        || order.getPaymentStatus() == PaymentStatus.paid;
  }

  private String paymentMethodLabel(PaymentMethod method) {
    return switch (method) {
      case cod -> "Cash on Delivery";
      case online -> "Online Payment";
      case bkash -> "bKash";
      case nagad -> "Nagad";
      case rocket -> "Rocket";
      case card -> "Card";
    };
  }

  private String paymentStatusLabel(LaundryOrder order) {
    if (isPaid(order)) {
      return "Paid / Confirmed";
    }
    if (order.getCodConfirmationStatus() == CodConfirmationStatus.customer_confirmed) {
      return "Customer Confirmed — awaiting delivery confirmation";
    }
    return switch (order.getPaymentStatus()) {
      case paid -> "Paid";
      case partial -> "Partially paid";
      case pending -> "Pending";
    };
  }

  private static Color parseColor(String hex, Color fallback) {
    if (hex == null || hex.isBlank()) {
      return fallback;
    }
    String s = hex.trim();
    if (s.startsWith("#")) {
      s = s.substring(1);
    }
    try {
      return new Color(Integer.parseInt(s, 16));
    } catch (NumberFormatException ex) {
      return fallback;
    }
  }

  private static String format(BigDecimal value) {
    return (value == null ? BigDecimal.ZERO : value).setScale(2, java.math.RoundingMode.HALF_UP)
        .toPlainString();
  }

  private static BigDecimal nullSafe(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }

  private static String safe(String value) {
    return (value == null || value.isBlank()) ? "—" : value;
  }

  private static String stripBlank(String value) {
    return (value == null || value.isBlank()) ? "" : value;
  }

  public String suggestFileName(LaundryOrder order, LocalDateTime now) {
    String date = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    return "receipt_" + order.getId() + "_" + date + ".pdf";
  }
}
