// repository/employee/EmployeeCreationRequestRepository.java
package com.company.hrpayroll.repository.employee;

import com.company.hrpayroll.entity.employee.EmployeeCreationRequest;
import com.company.hrpayroll.entity.employee.EmployeeRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmployeeCreationRequestRepository extends JpaRepository<EmployeeCreationRequest, Long> {

    List<EmployeeCreationRequest> findAllByOrderByCreatedAtDesc();

    List<EmployeeCreationRequest> findByStatusOrderByCreatedAtDesc(EmployeeRequestStatus status);

    List<EmployeeCreationRequest> findByRequestedByUserIdOrderByCreatedAtDesc(Long userId);

    boolean existsByNationalIdAndStatus(String nationalId, EmployeeRequestStatus status);

    long countByStatus(EmployeeRequestStatus status);
}