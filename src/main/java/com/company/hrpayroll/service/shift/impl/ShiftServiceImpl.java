// service/shift/impl/ShiftServiceImpl.java
package com.company.hrpayroll.service.shift.impl;

import com.company.hrpayroll.dto.request.shift.ShiftAssignmentRequest;
import com.company.hrpayroll.dto.request.shift.ShiftRequest;
import com.company.hrpayroll.dto.response.shift.ShiftAssignmentResponse;
import com.company.hrpayroll.dto.response.shift.ShiftResponse;
import com.company.hrpayroll.entity.shift.Shift;
import com.company.hrpayroll.entity.shift.ShiftAssignment;
import com.company.hrpayroll.exception.InvalidOperationException;
import com.company.hrpayroll.exception.ResourceNotFoundException;
import com.company.hrpayroll.mapper.shift.ShiftAssignmentMapper;
import com.company.hrpayroll.mapper.shift.ShiftMapper;
import com.company.hrpayroll.repository.branch.BranchRepository;
import com.company.hrpayroll.repository.department.DepartmentRepository;
import com.company.hrpayroll.repository.employee.EmployeeRepository;
import com.company.hrpayroll.repository.shift.ShiftAssignmentRepository;
import com.company.hrpayroll.repository.shift.ShiftRepository;
import com.company.hrpayroll.service.shift.ShiftService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ShiftServiceImpl implements ShiftService {

    private final ShiftRepository shiftRepository;
    private final ShiftAssignmentRepository shiftAssignmentRepository;
    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final BranchRepository branchRepository;
    private final ShiftMapper shiftMapper;
    private final ShiftAssignmentMapper shiftAssignmentMapper;

    @Override
    public ShiftResponse createShift(ShiftRequest request) {
        Shift shift = shiftMapper.toShift(request);
        Shift saved = shiftRepository.save(shift);
        return shiftMapper.toShiftResponse(saved);
    }

    @Override
    public ShiftResponse updateShift(Long id, ShiftRequest request) {
        Shift shift = shiftRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shift not found with id: " + id));
        shiftMapper.updateShiftFromRequest(shift, request);
        Shift saved = shiftRepository.save(shift);
        return shiftMapper.toShiftResponse(saved);
    }

    @Override
    public List<ShiftResponse> getAllShifts() {
        return shiftRepository.findAll().stream()
                .map(shiftMapper::toShiftResponse)
                .toList();
    }

    @Override
    public ShiftAssignmentResponse assignShift(ShiftAssignmentRequest request) {
        Shift shift = shiftRepository.findById(request.shiftId())
                .orElseThrow(() -> new ResourceNotFoundException("Shift not found with id: " + request.shiftId()));

        // Exactly one assignment level must be set — assigning a shift with
        // no target, or with two conflicting targets, is a request error.
        int targetsSet = countNonNull(request.employeeId(), request.departmentId(), request.branchId());
        if (targetsSet != 1) {
            throw new InvalidOperationException(
                    "Assign a shift to exactly one of: employee, department, or branch");
        }

        if (request.employeeId() != null && !employeeRepository.existsById(request.employeeId())) {
            throw new ResourceNotFoundException("Employee not found with id: " + request.employeeId());
        }
        if (request.departmentId() != null && !departmentRepository.existsById(request.departmentId())) {
            throw new ResourceNotFoundException("Department not found with id: " + request.departmentId());
        }
        if (request.branchId() != null && !branchRepository.existsById(request.branchId())) {
            throw new ResourceNotFoundException("Branch not found with id: " + request.branchId());
        }

        ShiftAssignment assignment = shiftAssignmentMapper.toShiftAssignment(request);
        ShiftAssignment saved = shiftAssignmentRepository.save(assignment);
        return shiftAssignmentMapper.toShiftAssignmentResponse(saved, shift.getName());
    }

    @Override
    public List<ShiftAssignmentResponse> getAssignmentsForEmployee(Long employeeId) {
        return shiftAssignmentRepository.findByEmployeeId(employeeId).stream()
                .map(this::toAssignmentResponse)
                .toList();
    }

    @Override
    public List<ShiftAssignmentResponse> getAssignmentsForBranch(Long branchId) {
        return shiftAssignmentRepository.findByBranchId(branchId).stream()
                .map(this::toAssignmentResponse)
                .toList();
    }

    private ShiftAssignmentResponse toAssignmentResponse(ShiftAssignment assignment) {
        String shiftName = shiftRepository.findById(assignment.getShiftId())
                .map(Shift::getName).orElse(null);
        return shiftAssignmentMapper.toShiftAssignmentResponse(assignment, shiftName);
    }

    private int countNonNull(Object... values) {
        int count = 0;
        for (Object value : values) {
            if (value != null) count++;
        }
        return count;
    }
}