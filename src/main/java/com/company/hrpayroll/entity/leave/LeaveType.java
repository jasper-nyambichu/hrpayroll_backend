// entity/leave/LeaveType.java
package com.company.hrpayroll.entity.leave;

import com.company.hrpayroll.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "leave_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveType extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String name; // ANNUAL, SICK, MATERNITY, PATERNITY, COMPASSIONATE, UNPAID

    @Builder.Default
    private Integer defaultEntitlementDays = 0;

    @Builder.Default
    private boolean paid = true;
}