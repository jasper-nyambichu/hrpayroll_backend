package com.company.hrpayroll.repository.auth;

import com.company.hrpayroll.entity.auth.PasswordReset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PasswordResetRepository extends JpaRepository<PasswordReset, Long> {

    Optional<PasswordReset> findFirstByUserIdAndUsedAtIsNullOrderByIdDesc(Long userId);

    List<PasswordReset> findByUserIdAndUsedAtIsNull(Long userId);

    Optional<PasswordReset> findByResetTokenHashAndUsedAtIsNull(String resetTokenHash);
}
