// entity/offday/OffDay.java
package com.company.hrpayroll.entity.offday;

import com.company.hrpayroll.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

// Days an agent gave one of their employees off. One day is a row whose two dates match.
@Entity
@Table(name = "off_days")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OffDay extends BaseEntity {

    @Column(nullable = false)
    private Long employeeId;

    @Column(nullable = false)
    private Long agentId;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    private String reason;
}
