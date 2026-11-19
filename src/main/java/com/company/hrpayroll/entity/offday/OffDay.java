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

// One day off of an employee (start and end are the same date; older rows may span a range). Given by their
// agent, or by the Branch Manager or Admin when the agent isn't available.
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

    // The employee's agent. Empty for an employee without one (the Admin or Branch Manager can still give them a day).
    private Long agentId;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    private String reason;
}
