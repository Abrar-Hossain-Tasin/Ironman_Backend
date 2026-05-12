package com.ironman.service;

import com.ironman.model.User;
import java.util.UUID;

/**
 * Outbound push notification transport. Concrete provider (FCM, web-push…)
 * should implement this. {@link NoOpPushService} is the default no-op.
 */
public interface PushService {
  void send(User user, String title, String body, String type, UUID referenceId);
}
