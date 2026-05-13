package com.ironman.config;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Backs /api/v1/services/slots. The lists drive what shows up in the order
 * wizard; capacityDefault is applied to every slot unless we add per-slot
 * overrides later. Edit application.yml to add/remove slots — no code change.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.slots")
public class SlotProperties {
  private int capacityDefault = 20;
  private List<String> pickup = new ArrayList<>();
  private List<String> delivery = new ArrayList<>();
}
