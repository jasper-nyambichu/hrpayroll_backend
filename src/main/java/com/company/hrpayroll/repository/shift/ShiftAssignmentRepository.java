// repository/shift/ShiftAssignmentRepository.java
package com.company.hrpayroll.repository.shift;

import com.company.hrpayroll.entity.shift.ShiftAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShiftAssignmentRepository extends JpaRepository<ShiftAssignment, Long> {

    List<ShiftAssignment> findByEmployeeId(Long employeeId);

    List<ShiftAssignment> findByDepartmentId(Long departmentId);

    List<ShiftAssignment> findByBranchId(Long branchId);

    List<ShiftAssignment> findByShiftId(Long shiftId);
}