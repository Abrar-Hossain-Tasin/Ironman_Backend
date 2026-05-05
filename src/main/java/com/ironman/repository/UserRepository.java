package com.ironman.repository;

import com.ironman.model.User;
import com.ironman.model.UserRole;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {
  Optional<User> findByEmailIgnoreCase(String email);

  boolean existsByEmailIgnoreCase(String email);

  List<User> findByRole(UserRole role);

  List<User> findByRoleIn(List<UserRole> roles);
}
