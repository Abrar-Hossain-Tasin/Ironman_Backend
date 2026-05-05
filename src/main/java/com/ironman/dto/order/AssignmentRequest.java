package com.ironman.dto.order;

import com.ironman.model.AssignmentType;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AssignmentRequest(
    @NotNull UUID assignedTo,
    @NotNull AssignmentType assignmentType,
    String notes
) {}
