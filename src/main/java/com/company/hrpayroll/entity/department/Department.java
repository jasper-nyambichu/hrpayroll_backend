// entity/department/Department.java
package com.company.hrpayroll.entity.department;

import com.company.hrpayroll.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Departments are company-wide — one list shared by every branch (e.g. Finance, Operations).
@Entity
@Table(name = "departments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Department extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    // No longer used: departments belong to the company, not to a branch. The column is kept
    // (nullable) so existing data and older clients keep working.
    private Long branchId;

    // The Employee.id of the head of department. Nullable.
    private Long headOfDepartmentId;
}