// repository/department/DepartmentRepository.java
package com.company.hrpayroll.repository.department;

import com.company.hrpayroll.entity.department.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    List<Department> findByBranchId(Long branchId);

    List<Department> findByHeadOfDepartmentId(Long employeeId);
}