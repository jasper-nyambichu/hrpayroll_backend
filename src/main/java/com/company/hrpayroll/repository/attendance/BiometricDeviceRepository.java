// repository/attendance/BiometricDeviceRepository.java
package com.company.hrpayroll.repository.attendance;

import com.company.hrpayroll.entity.attendance.BiometricDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BiometricDeviceRepository extends JpaRepository<BiometricDevice, Long> {

    Optional<BiometricDevice> findByDeviceSerial(String deviceSerial);

    List<BiometricDevice> findByBranchId(Long branchId);

    List<BiometricDevice> findByStatus(String status);
}