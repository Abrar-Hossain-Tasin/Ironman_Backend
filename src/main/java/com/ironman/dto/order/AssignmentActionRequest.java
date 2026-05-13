package com.ironman.dto.order;

import java.util.List;

public record AssignmentActionRequest(String notes, List<String> photoUrls) {
  public AssignmentActionRequest(String notes) {
    this(notes, List.of());
  }
}
