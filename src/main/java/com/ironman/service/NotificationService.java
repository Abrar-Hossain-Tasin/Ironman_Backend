//package com.ironman.service;
//
//import com.ironman.config.NotFoundException;
//import com.ironman.dto.common.NotificationResponse;
//import com.ironman.model.Notification;
//import com.ironman.model.User;
//import com.ironman.model.UserRole;
//import com.ironman.repository.NotificationRepository;
//import com.ironman.repository.UserRepository;
//import java.util.List;
//import java.util.UUID;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//@Service
//@RequiredArgsConstructor
//public class NotificationService {
//  private final NotificationRepository notificationRepository;
//  private final UserRepository userRepository;
//  private final PrincipalService principalService;
//
//  @Transactional
//  public void notifyUser(User user, String title, String body, String type, UUID referenceId) {
//    var notification = new Notification();
//    notification.setUser(user);
//    notification.setTitle(title);
//    notification.setBody(body);
//    notification.setType(type);
//    notification.setReferenceId(referenceId);
//    notificationRepository.save(notification);
//  }
//
//  @Transactional
//  public void notifyAdmins(String title, String body, String type, UUID referenceId) {
//    userRepository.findByRole(UserRole.admin)
//        .forEach(admin -> notifyUser(admin, title, body, type, referenceId));
//  }
//
//  public List<NotificationResponse> mine() {
//    return notificationRepository.findByUserIdOrderByCreatedAtDesc(principalService.currentUser().getId())
//        .stream()
//        .map(NotificationResponse::from)
//        .toList();
//  }
//
//  @Transactional
//  public NotificationResponse markRead(UUID id) {
//    User user = principalService.currentUser();
//    Notification notification = notificationRepository.findById(id)
//        .filter(item -> item.getUser().getId().equals(user.getId()))
//        .orElseThrow(() -> new NotFoundException("Notification not found"));
//    notification.setRead(true);
//    return NotificationResponse.from(notificationRepository.save(notification));
//  }
//
//  @Transactional
//  public void markAllRead() {
//    User user = principalService.currentUser();
//    notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).forEach(notification -> {
//      notification.setRead(true);
//      notificationRepository.save(notification);
//    });
//  }
//}

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
  private final EmailService emailService;
  private final SmsService smsService;
  private final PushService pushService;

  @Transactional
  public void notifyUser(User user, String title, String body,
                         String type, UUID referenceId) {
    // 1. Save in-app notification (powers Supabase Realtime fan-out + history)
    var notification = new Notification();
    notification.setUser(user);
    notification.setTitle(title);
    notification.setBody(body);
    notification.setType(type);
    notification.setReferenceId(referenceId);
    notificationRepository.save(notification);

    // 2. Out-of-band channels — no-op stubs unless a provider is configured.
    emailService.send(user.getEmail(), title, body);
    if (user.getPhone() != null && !user.getPhone().isBlank()) {
      smsService.send(user.getPhone(), title + ": " + body);
    }
    pushService.send(user, title, body, type, referenceId);
  }

  @Transactional
  public void notifyAdmins(String title, String body, String type, UUID referenceId) {
    userRepository.findByRole(UserRole.admin)
            .forEach(admin -> notifyUser(admin, title, body, type, referenceId));
  }

  /**
   * Send a one-off broadcast to every active user, optionally filtered to a
   * single role. Used by the admin broadcasts UI for system announcements
   * (e.g. service notices, promo launches). Returns the recipient count.
   */
  @Transactional
  public int broadcast(String title, String body, UserRole role) {
    var recipients = role == null
            ? userRepository.findAll()
            : userRepository.findByRole(role);
    int count = 0;
    for (User user : recipients) {
      if (!user.isActive()) continue;
      notifyUser(user, title, body, "broadcast", null);
      count++;
    }
    return count;
  }

  public List<NotificationResponse> mine() {
    return notificationRepository
            .findByUserIdOrderByCreatedAtDesc(principalService.currentUser().getId())
            .stream().map(NotificationResponse::from).toList();
  }

  @Transactional
  public NotificationResponse markRead(UUID id) {
    User user = principalService.currentUser();
    Notification n = notificationRepository.findById(id)
            .filter(item -> item.getUser().getId().equals(user.getId()))
            .orElseThrow(() -> new NotFoundException("Notification not found"));
    n.setRead(true);
    return NotificationResponse.from(notificationRepository.save(n));
  }

  @Transactional
  public void markAllRead() {
    User user = principalService.currentUser();
    notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
            .forEach(n -> { n.setRead(true); notificationRepository.save(n); });
  }
}
