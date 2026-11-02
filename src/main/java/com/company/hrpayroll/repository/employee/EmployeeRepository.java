// repository/employee/EmployeeRepository.java
package com.company.hrpayroll.repository.employee;

import com.company.hrpayroll.entity.employee.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// A deleted employee stays in the table so payslips, payments, loans and damages keep their name.
// Every list here leaves them out; findById and findAllById still find them for those records.
@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByEmployeeNumber(String employeeNumber);

    boolean existsByEmployeeNumber(String employeeNumber);

    boolean existsByNationalId(String nationalId);

    @Query("select e from Employee e where e.branchId = :branchId and e.employmentStatus <> 'DELETED'")
    List<Employee> findByBranchId(@Param("branchId") Long branchId);

    @Query("select e from Employee e where e.agentId = :agentId and e.employmentStatus <> 'DELETED'")
    List<Employee> findByAgentId(@Param("agentId") Long agentId);

    @Query("select e from Employee e where e.branchId = :branchId and e.agentId = :agentId and e.employmentStatus <> 'DELETED'")
    List<Employee> findByBranchIdAndAgentId(@Param("branchId") Long branchId, @Param("agentId") Long agentId);

    @Query("select e from Employee e where e.departmentId = :departmentId and e.employmentStatus <> 'DELETED'")
    List<Employee> findByDepartmentId(@Param("departmentId") Long departmentId);

    @Query("select e from Employee e where e.branchId in :branchIds and e.employmentStatus <> 'DELETED'")
    List<Employee> findByBranchIdIn(@Param("branchIds") List<Long> branchIds);

    @Query("select e from Employee e where e.agentId in :agentIds and e.employmentStatus <> 'DELETED'")
    List<Employee> findByAgentIdIn(@Param("agentIds") List<Long> agentIds);

    List<Employee> findByEmploymentStatus(String employmentStatus);

    long countByAgentIdAndEmploymentStatus(Long agentId, String employmentStatus);

    @Query("select e from Employee e where e.employmentStatus <> 'DELETED'")
    List<Employee> findAllExisting();

    @Query("select count(e) from Employee e where e.employmentStatus <> 'DELETED'")
    long countExisting();

    // Two steps because the codes are unique and one row's new code can be another row's current one.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "UPDATE employees SET employee_number = 'TMP-' || id WHERE employment_status <> 'DELETED'", nativeQuery = true)
    void clearEmployeeNumbers();

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "UPDATE employees e SET employee_number = 'EMP-' || lpad(CAST(r.position AS TEXT), GREATEST(4, length(CAST(r.position AS TEXT))), '0') "
            + "FROM (SELECT id, row_number() OVER (ORDER BY id) AS position FROM employees WHERE employment_status <> 'DELETED') r "
            + "WHERE e.id = r.id", nativeQuery = true)
    void assignEmployeeNumbersInOrder();
}
