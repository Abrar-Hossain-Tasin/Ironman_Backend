package com.ironman.controller;

import com.ironman.dto.common.ApiMessage;
import com.ironman.dto.common.NotificationPreferenceResponse;
import com.ironman.dto.common.NotificationResponse;
import com.ironman.dto.common.UpdateNotificationPreferenceRequest;
import com.ironman.service.NotificationService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {
  private final NotificationService notificationService;

  @GetMapping
  public List<NotificationResponse> mine() {
    return notificationService.mine();
  }

  @PutMapping("/{id}/read")
  public NotificationResponse read(@PathVariable UUID id) {
    return notificationService.markRead(id);
  }

  @PutMapping("/read-all")
  public ApiMessage readAll() {
    notificationService.markAllRead();
    return new ApiMessage("All notifications marked read");
  }

  @GetMapping("/preferences")
  public List<NotificationPreferenceResponse> preferences() {
    return notificationService.preferences();
  }

  @PutMapping("/preferences")
  public NotificationPreferenceResponse updatePreference(
      @Valid @RequestBody UpdateNotificationPreferenceRequest request) {
    return notificationService.updatePreference(request);
  }
}
