// entity/employee/EmployeeDocument.java
package com.company.hrpayroll.entity.employee;

import com.company.hrpayroll.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "employee_documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeDocument extends BaseEntity {

    @Column(nullable = false)
    private Long employeeId;

    @Column(nullable = false, length = 30)
    private String documentType; // CONTRACT, ID, CERTIFICATE, OTHER

    @Column(nullable = false)
    private String fileUrl;

    private String fileName;
}