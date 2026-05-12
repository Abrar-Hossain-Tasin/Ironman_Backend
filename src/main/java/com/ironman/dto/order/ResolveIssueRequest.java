package com.ironman.dto.order;

import com.ironman.model.IssueStatus;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ResolveIssueRequest(
    @NotNull IssueStatus status,
    String resolutionNotes,
    BigDecimal refundAmount
) {}
