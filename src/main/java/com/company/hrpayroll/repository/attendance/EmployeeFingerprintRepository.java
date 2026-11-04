package com.company.hrpayroll.repository.attendance;

import com.company.hrpayroll.entity.attendance.EmployeeFingerprint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeFingerprintRepository extends JpaRepository<EmployeeFingerprint, Long> {

    Optional<EmployeeFingerprint> findByEmployeeIdAndFingerSlot(Long employeeId, Short fingerSlot);

    List<EmployeeFingerprint> findByEmployeeIdIn(List<Long> employeeIds);

    long countByEmployeeId(Long employeeId);

    void deleteByEmployeeId(Long employeeId);
}
