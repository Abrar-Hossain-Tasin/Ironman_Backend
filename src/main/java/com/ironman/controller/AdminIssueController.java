package com.ironman.controller;

import com.ironman.dto.order.IssueResponse;
import com.ironman.dto.order.ResolveIssueRequest;
import com.ironman.model.IssueStatus;
import com.ironman.service.IssueService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/v1/admin/issues")
@RequiredArgsConstructor
public class AdminIssueController {

  private final IssueService issueService;

  @GetMapping
  public List<IssueResponse> list(@RequestParam(required = false) IssueStatus status) {
    return issueService.listAdmin(status);
  }

  @PatchMapping("/{id}")
  public IssueResponse resolve(@PathVariable UUID id,
                               @Valid @RequestBody ResolveIssueRequest request) {
    return issueService.resolve(id, request);
  }
}
