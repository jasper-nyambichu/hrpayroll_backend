// repository/damage/DamageRepository.java
package com.company.hrpayroll.repository.damage;

import com.company.hrpayroll.entity.damage.Damage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DamageRepository extends JpaRepository<Damage, Long> {

    List<Damage> findByStatus(String status);

    List<Damage> findByEmployeeId(Long employeeId);

    List<Damage> findByEmployeeIdAndStatus(Long employeeId, String status);

    List<Damage> findByEmployeeIdInAndStatus(List<Long> employeeIds, String status);

    Optional<Damage> findByConsentEventId(String consentEventId);
    List<Damage> findByPayrollRunIdIn(List<Long> payrollRunIds);
}
