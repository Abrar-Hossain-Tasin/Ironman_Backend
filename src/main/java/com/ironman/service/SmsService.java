package com.ironman.service;

/**
 * Outbound SMS transport. Concrete provider (Twilio, SSL Wireless, Infobip…)
 * should implement this interface and be marked with @Primary or selected via
 * configuration. The default {@link NoOpSmsService} just logs.
 */
public interface SmsService {
  void send(String phone, String message);
}
