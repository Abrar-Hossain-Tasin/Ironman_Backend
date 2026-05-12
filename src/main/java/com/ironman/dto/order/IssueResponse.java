package com.ironman.dto.order;

import com.ironman.model.IssueStatus;
import com.ironman.model.IssueType;
import com.ironman.model.OrderIssue;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record IssueResponse(
    UUID id,
    UUID orderId,
    UUID reportedBy,
    String reportedByName,
    IssueType type,
    String description,
    List<String> photoUrls,
    IssueStatus status,
    String resolutionNotes,
    BigDecimal refundAmount,
    UUID resolvedBy,
    Instant createdAt,
    Instant resolvedAt
) {
  public static IssueResponse from(OrderIssue issue) {
    String raw = issue.getPhotoUrls();
    List<String> photos = (raw == null || raw.isBlank())
        ? List.of()
        : List.of(raw.split("\\s*,\\s*"));
    return new IssueResponse(
        issue.getId(),
        issue.getOrder().getId(),
        issue.getReportedBy().getId(),
        issue.getReportedBy().getFullName(),
        issue.getType(),
        issue.getDescription(),
        photos,
        issue.getStatus(),
        issue.getResolutionNotes(),
        issue.getRefundAmount(),
        issue.getResolvedBy() == null ? null : issue.getResolvedBy().getId(),
        issue.getCreatedAt(),
        issue.getResolvedAt()
    );
  }
}
