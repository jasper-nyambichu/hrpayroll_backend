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

    // Held while a payment is recorded, so two people in the Accounts Office cannot pay the same invoice twice.
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from PaymentRequest p where p.id = :id")
    java.util.Optional<PaymentRequest> findForUpdate(Long id);

    // The same M-Pesa code, or the same cheque of the same bank, cannot prove two payments.
    boolean existsByPaymentMethodAndReleaseReferenceIgnoreCase(String paymentMethod, String releaseReference);

    boolean existsByPaymentMethodAndChequeBankIgnoreCaseAndChequeNumberIgnoreCase(
            String paymentMethod, String chequeBank, String chequeNumber);
}
