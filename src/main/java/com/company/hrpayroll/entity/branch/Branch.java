// entity/branch/Branch.java
package com.company.hrpayroll.entity.branch;

import com.company.hrpayroll.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "branches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Branch extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    private String address;
    private String phone;
    private String email;

    // The User.id of the Branch Manager assigned here. Nullable — a branch
    // can exist before a manager is assigned.
    private Long managerId;

    @Builder.Default
    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";
}