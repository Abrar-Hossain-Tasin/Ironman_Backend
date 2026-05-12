package com.ironman.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Default SMS transport that simply logs. Active until a real provider
 * implementation is added.
 */
@Slf4j
@Service
@ConditionalOnMissingBean(value = SmsService.class, ignored = NoOpSmsService.class)
@ConditionalOnProperty(name = "app.sms.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpSmsService implements SmsService {
  @Override
  public void send(String phone, String message) {
    log.info("[SMS SKIPPED] To: {} | Body: {}", phone, message);
  }
}
