package com.ironman.dto.order;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

public record BatchAssignmentActionRequest(
    @NotEmpty List<UUID> assignmentIds,
    String notes,
    List<String> photoUrls
) {}
