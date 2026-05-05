package com.ironman.controller;

import com.ironman.dto.order.AssignmentActionRequest;
import com.ironman.dto.order.AssignmentResponse;
import com.ironman.service.AssignmentService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@PreAuthorize("hasAnyRole('WASH_MAN','IRON_MAN','DRY_CLEAN_MAN')")
@RestController
@RequestMapping("/api/v1/worker")
@RequiredArgsConstructor
public class WorkerController {
  private final AssignmentService assignmentService;

  @GetMapping("/assignments")
  public List<AssignmentResponse> assignments() {
    return assignmentService.mine();
  }

  @PutMapping("/assignments/{id}/start")
  public AssignmentResponse start(@PathVariable UUID id) {
    return assignmentService.start(id);
  }

  @PutMapping("/assignments/{id}/complete")
  public AssignmentResponse complete(@PathVariable UUID id, @RequestBody(required = false) AssignmentActionRequest request) {
    return assignmentService.complete(id, request);
  }
}
