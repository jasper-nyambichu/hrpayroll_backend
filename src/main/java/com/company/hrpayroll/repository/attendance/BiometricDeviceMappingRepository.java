package com.company.hrpayroll.repository.attendance;

import com.company.hrpayroll.entity.attendance.BiometricDeviceMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BiometricDeviceMappingRepository extends JpaRepository<BiometricDeviceMapping, Long> {

    Optional<BiometricDeviceMapping> findByDeviceIdAndTerminalUserId(Long deviceId, String terminalUserId);

    boolean existsByDeviceIdAndTerminalUserId(Long deviceId, String terminalUserId);

    List<BiometricDeviceMapping> findByDeviceId(Long deviceId);
}