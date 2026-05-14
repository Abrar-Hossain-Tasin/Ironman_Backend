package com.ironman.model;

public enum PaymentWebhookStatus {
  received,
  processed,
  duplicate,
  retry_scheduled,
  failed
}
