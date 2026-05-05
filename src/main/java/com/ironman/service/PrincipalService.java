package com.ironman.service;

import com.ironman.model.User;
import com.ironman.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PrincipalService {
  private final UserRepository userRepository;

  public User currentUser() {
    Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    if (principal instanceof User user) {
      return user;
    }
    return userRepository.findByEmailIgnoreCase(String.valueOf(principal))
        .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
  }
}
