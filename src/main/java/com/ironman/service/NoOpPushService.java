package com.ironman.service;

import com.ironman.model.User;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@ConditionalOnMissingBean(value = PushService.class, ignored = NoOpPushService.class)
@ConditionalOnProperty(name = "app.push.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpPushService implements PushService {
  @Override
  public void send(User user, String title, String body, String type, UUID referenceId) {
    log.info("[PUSH SKIPPED] To: {} | {} — {}", user.getId(), title, body);
  }
}
