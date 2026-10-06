package com.company.hrpayroll.repository.attendance;

import com.company.hrpayroll.entity.attendance.BiometricSyncFailure;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BiometricSyncFailureRepository extends JpaRepository<BiometricSyncFailure, Long> {

    List<BiometricSyncFailure> findTop100ByOrderByCreatedAtDesc();
}