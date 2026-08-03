// controller/shift/ShiftController.java
package com.company.hrpayroll.controller.shift;

import com.company.hrpayroll.audit.Auditable;
import com.company.hrpayroll.dto.request.shift.ShiftAssignmentRequest;
import com.company.hrpayroll.dto.request.shift.ShiftRequest;
import com.company.hrpayroll.dto.response.ApiResponse;
import com.company.hrpayroll.dto.response.shift.ShiftAssignmentResponse;
import com.company.hrpayroll.dto.response.shift.ShiftResponse;
import com.company.hrpayroll.service.shift.ShiftService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/shifts")
@RequiredArgsConstructor
public class ShiftController {

    private final ShiftService shiftService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER')")
    @Auditable(action = "SHIFT_CREATED", entityType = "Shift")
    public ApiResponse<ShiftResponse> createShift(@Valid @RequestBody ShiftRequest request) {
        return ApiResponse.success("Shift created", shiftService.createShift(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER')")
    @Auditable(action = "SHIFT_UPDATED", entityType = "Shift")
    public ApiResponse<ShiftResponse> updateShift(@PathVariable Long id, @Valid @RequestBody ShiftRequest request) {
        return ApiResponse.success("Shift updated", shiftService.updateShift(id, request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER', 'BRANCH_MANAGER')")
    public ApiResponse<List<ShiftResponse>> getAllShifts() {
        return ApiResponse.success(shiftService.getAllShifts());
    }

    @PostMapping("/assignments")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER')")
    @Auditable(action = "SHIFT_ASSIGNED", entityType = "ShiftAssignment")
    public ApiResponse<ShiftAssignmentResponse> assignShift(@Valid @RequestBody ShiftAssignmentRequest request) {
        return ApiResponse.success("Shift assigned", shiftService.assignShift(request));
    }

    @GetMapping("/assignments/employee/{employeeId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER', 'BRANCH_MANAGER') " +
            "or @accessGuard.isSameEmployee(#employeeId)")
    public ApiResponse<List<ShiftAssignmentResponse>> getAssignmentsForEmployee(@PathVariable Long employeeId) {
        return ApiResponse.success(shiftService.getAssignmentsForEmployee(employeeId));
    }

    @GetMapping("/assignments/branch/{branchId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER') or @accessGuard.isSameBranch(#branchId)")
    public ApiResponse<List<ShiftAssignmentResponse>> getAssignmentsForBranch(@PathVariable Long branchId) {
        return ApiResponse.success(shiftService.getAssignmentsForBranch(branchId));
    }
}