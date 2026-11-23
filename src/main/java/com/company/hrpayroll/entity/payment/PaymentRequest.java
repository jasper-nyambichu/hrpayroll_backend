// entity/payment/PaymentRequest.java
package com.company.hrpayroll.entity.payment;

import com.company.hrpayroll.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

// Money an agent asks for so they can pay their employees. PENDING until the Admin decides;
// APPROVED means the Admin should release it; RELEASED means the money has gone out and the
// agent may now record each payment (with its M-Pesa code) against it.
@Entity
@Table(name = "payment_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequest extends BaseEntity {

    @Column(nullable = false)
    private Long agentId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 500)
    private String purpose;

    @Builder.Default
    @Column(nullable = false, length = 20)
    private String status = "PENDING"; // PENDING, APPROVED, REJECTED, RELEASED, CANCELLED

    @Column(nullable = false)
    private Long requestedByUserId;

    private Long decidedByUserId;
    private LocalDateTime decidedAt;

    @Column(length = 500)
    private String rejectionReason;

    private Long releasedByUserId;
    private LocalDateTime releasedAt;

    // How the money was sent (bank reference, M-Pesa transaction code of the transfer, cheque number...).
    // For an M-Pesa payment it is the transaction code itself; otherwise a one-line summary.
    @Column(length = 100)
    private String releaseReference;

    // ---- How the Accounts Office paid it (V39). Empty for payments released before that. ----

    @Column(length = 20)
    private String paymentMethod;      // MPESA, CHEQUE or MANUAL

    @Column(length = 30)
    private String chequeNumber;

    @Column(length = 100)
    private String chequeBank;

    private LocalDate chequeDate;

    private LocalDate paidOn;

    @Column(length = 500)
    private String paymentNote;
}
