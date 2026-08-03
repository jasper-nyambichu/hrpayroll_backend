// service/shift/ShiftService.java
package com.company.hrpayroll.service.shift;

import com.company.hrpayroll.dto.request.shift.ShiftAssignmentRequest;
import com.company.hrpayroll.dto.request.shift.ShiftRequest;
import com.company.hrpayroll.dto.response.shift.ShiftAssignmentResponse;
import com.company.hrpayroll.dto.response.shift.ShiftResponse;

import java.util.List;

public interface ShiftService {
    ShiftResponse createShift(ShiftRequest request);
    ShiftResponse updateShift(Long id, ShiftRequest request);
    List<ShiftResponse> getAllShifts();

    ShiftAssignmentResponse assignShift(ShiftAssignmentRequest request);
    List<ShiftAssignmentResponse> getAssignmentsForEmployee(Long employeeId);
    List<ShiftAssignmentResponse> getAssignmentsForBranch(Long branchId);
}