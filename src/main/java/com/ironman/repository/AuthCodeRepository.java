package com.ironman.repository;

import com.ironman.model.AuthCode;
import com.ironman.model.AuthCodePurpose;
import com.ironman.model.User;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthCodeRepository extends JpaRepository<AuthCode, UUID> {

  Optional<AuthCode> findTopByUserAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
      User user, AuthCodePurpose purpose);

  List<AuthCode> findByUserAndPurposeAndConsumedAtIsNull(User user, AuthCodePurpose purpose);

  long deleteByExpiresAtBefore(Instant cutoff);
}
