// mapper/shift/ShiftAssignmentMapper.java
package com.company.hrpayroll.mapper.shift;

import com.company.hrpayroll.dto.request.shift.ShiftAssignmentRequest;
import com.company.hrpayroll.dto.response.shift.ShiftAssignmentResponse;
import com.company.hrpayroll.entity.shift.ShiftAssignment;
import org.springframework.stereotype.Service;

@Service
public class ShiftAssignmentMapper {

    public ShiftAssignment toShiftAssignment(ShiftAssignmentRequest dto) {
        return ShiftAssignment.builder()
                .shiftId(dto.shiftId())
                .employeeId(dto.employeeId())
                .departmentId(dto.departmentId())
                .branchId(dto.branchId())
                .build();
    }

    public ShiftAssignmentResponse toShiftAssignmentResponse(ShiftAssignment assignment, String shiftName) {
        return new ShiftAssignmentResponse(
                assignment.getId(),
                assignment.getShiftId(),
                shiftName,
                assignment.getEmployeeId(),
                assignment.getDepartmentId(),
                assignment.getBranchId()
        );
    }
}