package com.ironman.controller;

import com.ironman.dto.common.ApiMessage;
import com.ironman.dto.common.BroadcastRequest;
import com.ironman.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/v1/admin/notifications")
@RequiredArgsConstructor
public class AdminBroadcastController {

  private final NotificationService notificationService;

  @PostMapping("/broadcast")
  public ApiMessage broadcast(@Valid @RequestBody BroadcastRequest request) {
    int recipients = notificationService.broadcast(
        request.title().trim(),
        request.body().trim(),
        request.role()
    );
    return new ApiMessage("Sent to " + recipients + " recipient(s)");
  }
}
