package com.ironman.model;

public enum OrderStatus {
  pending,
  confirmed,
  pickup_assigned,
  picked_up,
  in_wash,
  wash_complete,
  in_dry_clean,
  dry_clean_complete,
  waiting_for_iron,
  in_iron,
  iron_complete,
  ready,
  delivery_assigned,
  out_for_delivery,
  delivered,
  delivery_failed,
  returned,
  disputed,
  cancelled
}
