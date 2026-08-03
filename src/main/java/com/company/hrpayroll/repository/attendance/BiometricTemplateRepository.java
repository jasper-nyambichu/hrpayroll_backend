// repository/attendance/BiometricTemplateRepository.java
package com.company.hrpayroll.repository.attendance;

import com.company.hrpayroll.entity.attendance.BiometricTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BiometricTemplateRepository extends JpaRepository<BiometricTemplate, Long> {

    // The full local cache a branch's bridge-agent pulls down.
    List<BiometricTemplate> findByBranchId(Long branchId);

    List<BiometricTemplate> findByEmployeeId(Long employeeId);

    boolean existsByEmployeeIdAndFingerIndex(Long employeeId, Integer fingerIndex);

    long countByEmployeeId(Long employeeId);
}