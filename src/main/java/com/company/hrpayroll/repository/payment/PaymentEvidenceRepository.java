// repository/payment/PaymentEvidenceRepository.java
package com.company.hrpayroll.repository.payment;

import com.company.hrpayroll.entity.payment.PaymentEvidence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentEvidenceRepository extends JpaRepository<PaymentEvidence, Long> {

    Optional<PaymentEvidence> findByPaymentRequestId(Long paymentRequestId);

    boolean existsByPaymentRequestId(Long paymentRequestId);

    // Which of these payments have proof, without loading the files.
    @org.springframework.data.jpa.repository.Query(
            "select e.paymentRequestId from PaymentEvidence e where e.paymentRequestId in :ids")
    List<Long> findPaymentRequestIdsWithEvidence(Collection<Long> ids);
}
