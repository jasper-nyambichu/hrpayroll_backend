// entity/payment/PaymentEvidence.java
package com.company.hrpayroll.entity.payment;

import com.company.hrpayroll.entity.BaseEntity;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// The proof of one agent payment (receipt, M-Pesa message screenshot, cheque photo), uploaded by the Accounts
// Office when it records the payment. Kept in the database so it is in every backup (V39).
@Entity
@Table(name = "payment_evidence")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentEvidence extends BaseEntity {

    @Column(nullable = false, unique = true)
    private Long paymentRequestId;

    @Column(nullable = false, length = 200)
    private String fileName;

    @Column(nullable = false, length = 100)
    private String contentType;

    @Basic(fetch = FetchType.LAZY)
    @Column(nullable = false, columnDefinition = "bytea")
    private byte[] data;

    private Long uploadedByUserId;
}
