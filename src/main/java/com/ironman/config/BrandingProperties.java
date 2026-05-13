package com.ironman.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.branding")
public class BrandingProperties {
  private String name = "Ironman Laundry";
  private String tagline = "Premium laundry, delivered.";
  private String themeColor = "#F5A623";
  private String logoUrl;
  private String companyAddress;
  private String contactPhone;
  private String contactEmail;
  private String website;
}
