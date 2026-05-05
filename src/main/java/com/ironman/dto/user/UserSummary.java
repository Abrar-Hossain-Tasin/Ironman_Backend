package com.ironman.dto.user;

import com.ironman.model.User;
import com.ironman.model.UserRole;
import java.util.UUID;

public record UserSummary(
    UUID id,
    String fullName,
    String email,
    String phone,
    UserRole role,
    String profilePictureUrl,
    boolean active
) {
  public static UserSummary from(User user) {
    return new UserSummary(
        user.getId(),
        user.getFullName(),
        user.getEmail(),
        user.getPhone(),
        user.getRole(),
        user.getProfilePictureUrl(),
        user.isActive()
    );
  }
}
