// repository/payment/EmployeePaymentRepository.java
package com.company.hrpayroll.repository.payment;

import com.company.hrpayroll.entity.payment.EmployeePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmployeePaymentRepository extends JpaRepository<EmployeePayment, Long> {

    List<EmployeePayment> findByPaymentRequestId(Long paymentRequestId);

    List<EmployeePayment> findByAgentId(Long agentId);

    List<EmployeePayment> findByAgentIdAndPaymentRequestId(Long agentId, Long paymentRequestId);

    List<EmployeePayment> findByEmployeeId(Long employeeId);

    boolean existsByMpesaCode(String mpesaCode);
}
