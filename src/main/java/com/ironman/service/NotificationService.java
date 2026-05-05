package com.ironman.service;

import com.ironman.config.NotFoundException;
import com.ironman.dto.common.NotificationResponse;
import com.ironman.model.Notification;
import com.ironman.model.User;
import com.ironman.model.UserRole;
import com.ironman.repository.NotificationRepository;
import com.ironman.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {
  private final NotificationRepository notificationRepository;
  private final UserRepository userRepository;
  private final PrincipalService principalService;

  @Transactional
  public void notifyUser(User user, String title, String body, String type, UUID referenceId) {
    var notification = new Notification();
    notification.setUser(user);
    notification.setTitle(title);
    notification.setBody(body);
    notification.setType(type);
    notification.setReferenceId(referenceId);
    notificationRepository.save(notification);
  }

  @Transactional
  public void notifyAdmins(String title, String body, String type, UUID referenceId) {
    userRepository.findByRole(UserRole.admin)
        .forEach(admin -> notifyUser(admin, title, body, type, referenceId));
  }

  public List<NotificationResponse> mine() {
    return notificationRepository.findByUserIdOrderByCreatedAtDesc(principalService.currentUser().getId())
        .stream()
        .map(NotificationResponse::from)
        .toList();
  }

  @Transactional
  public NotificationResponse markRead(UUID id) {
    User user = principalService.currentUser();
    Notification notification = notificationRepository.findById(id)
        .filter(item -> item.getUser().getId().equals(user.getId()))
        .orElseThrow(() -> new NotFoundException("Notification not found"));
    notification.setRead(true);
    return NotificationResponse.from(notificationRepository.save(notification));
  }

  @Transactional
  public void markAllRead() {
    User user = principalService.currentUser();
    notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).forEach(notification -> {
      notification.setRead(true);
      notificationRepository.save(notification);
    });
  }
}
