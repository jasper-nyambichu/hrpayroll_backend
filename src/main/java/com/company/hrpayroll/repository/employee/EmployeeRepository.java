// repository/employee/EmployeeRepository.java
package com.company.hrpayroll.repository.employee;

import com.company.hrpayroll.entity.employee.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByEmployeeNumber(String employeeNumber);

    boolean existsByEmployeeNumber(String employeeNumber);

    boolean existsByNationalId(String nationalId);

    List<Employee> findByBranchId(Long branchId);

    List<Employee> findByAgentId(Long agentId);

    List<Employee> findByBranchIdAndAgentId(Long branchId, Long agentId);

    List<Employee> findByDepartmentId(Long departmentId);

    List<Employee> findByBranchIdIn(List<Long> branchIds);

    List<Employee> findByAgentIdIn(List<Long> agentIds);

    List<Employee> findByEmploymentStatus(String employmentStatus);

    long countByAgentIdAndEmploymentStatus(Long agentId, String employmentStatus);
}