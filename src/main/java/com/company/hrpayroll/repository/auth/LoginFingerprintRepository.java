package com.company.hrpayroll.repository.auth;

import com.company.hrpayroll.entity.auth.LoginFingerprint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface LoginFingerprintRepository extends JpaRepository<LoginFingerprint, Long> {

    Optional<LoginFingerprint> findByUserId(Long userId);

    List<LoginFingerprint> findByUserIdIn(Collection<Long> userIds);

    boolean existsByUserId(Long userId);
}
