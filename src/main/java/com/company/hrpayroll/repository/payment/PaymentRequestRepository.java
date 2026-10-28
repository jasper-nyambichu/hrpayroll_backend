// repository/payment/PaymentRequestRepository.java
package com.company.hrpayroll.repository.payment;

import com.company.hrpayroll.entity.payment.PaymentRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRequestRepository extends JpaRepository<PaymentRequest, Long> {

    List<PaymentRequest> findByAgentId(Long agentId);

    List<PaymentRequest> findByStatus(String status);
}
