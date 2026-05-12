package com.ironman.dto.order;

import com.ironman.model.IssueType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CreateIssueRequest(
    @NotNull IssueType type,
    @NotBlank String description,
    List<String> photoUrls
) {}
