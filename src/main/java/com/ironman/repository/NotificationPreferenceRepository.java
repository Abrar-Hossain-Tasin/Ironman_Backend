package com.ironman.repository;

import com.ironman.model.NotificationPreference;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, UUID> {
  List<NotificationPreference> findByUserIdOrderByChannelAscNotificationTypeAsc(UUID userId);

  Optional<NotificationPreference> findByUserIdAndChannelAndNotificationType(
      UUID userId, String channel, String notificationType);
}
