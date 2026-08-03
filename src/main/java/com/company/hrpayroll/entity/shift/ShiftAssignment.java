// entity/shift/ShiftAssignment.java
package com.company.hrpayroll.entity.shift;

import com.company.hrpayroll.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "shift_assignments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShiftAssignment extends BaseEntity {

    @Column(nullable = false)
    private Long shiftId;

    // Exactly one of these three is set, depending on assignment level.
    private Long employeeId;
    private Long departmentId;
    private Long branchId;
}