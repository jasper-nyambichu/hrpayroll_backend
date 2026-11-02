// repository/settlement/EmployeePayslipRepository.java
package com.company.hrpayroll.repository.settlement;

import com.company.hrpayroll.entity.settlement.EmployeePayslip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface EmployeePayslipRepository extends JpaRepository<EmployeePayslip, Long> {

    List<EmployeePayslip> findByEmployeeIdIn(Collection<Long> employeeIds);

    List<EmployeePayslip> findBySettlementIdIn(Collection<Long> settlementIds);
}
