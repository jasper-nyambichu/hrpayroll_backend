// repository/employee/EmployeeDocumentRepository.java
package com.company.hrpayroll.repository.employee;

import com.company.hrpayroll.entity.employee.EmployeeDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmployeeDocumentRepository extends JpaRepository<EmployeeDocument, Long> {

    List<EmployeeDocument> findByEmployeeId(Long employeeId);

    List<EmployeeDocument> findByEmployeeIdAndDocumentType(Long employeeId, String documentType);
}