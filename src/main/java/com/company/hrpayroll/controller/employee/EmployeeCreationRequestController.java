// controller/employee/EmployeeCreationRequestController.java
package com.company.hrpayroll.controller.employee;

import com.company.hrpayroll.audit.Auditable;
import com.company.hrpayroll.dto.request.employee.EmployeeRequest;
import com.company.hrpayroll.dto.request.employee.EmployeeRequestRejection;
import com.company.hrpayroll.dto.response.ApiResponse;
import com.company.hrpayroll.dto.response.employee.EmployeeCreationRequestResponse;
import com.company.hrpayroll.entity.employee.EmployeeRequestStatus;
import com.company.hrpayroll.security.AccessGuard;
import com.company.hrpayroll.service.employee.EmployeeCreationRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// A Branch Manager or an Agent (or the legacy HR role) proposes a new employee; an Admin
// approves or rejects. Approval is what actually creates the Employee row.
@RestController
@RequestMapping("/employee-requests")
@RequiredArgsConstructor
public class EmployeeCreationRequestController {

    private static final String SUBMITTER_ROLES = "hasAnyRole('HR_MANAGER', 'BRANCH_MANAGER', 'AGENT')";

    private final EmployeeCreationRequestService requestService;
    private final AccessGuard accessGuard;

    @PostMapping
    @PreAuthorize(SUBMITTER_ROLES)
    @Auditable(action = "EMPLOYEE_CREATION_REQUESTED", entityType = "EmployeeCreationRequest")
    public ApiResponse<EmployeeCreationRequestResponse> submit(@Valid @RequestBody EmployeeRequest request) {
        return ApiResponse.success(
                "Request submitted for admin approval",
                requestService.submit(request, accessGuard.currentUserId()));
    }

    // Admin queue. Optional ?status=PENDING|APPROVED|REJECTED
    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ApiResponse<List<EmployeeCreationRequestResponse>> list(
            @RequestParam(required = false) EmployeeRequestStatus status
    ) {
        return ApiResponse.success(requestService.list(status));
    }

    // The submitter's own requests and their outcome.
    @GetMapping("/mine")
    @PreAuthorize(SUBMITTER_ROLES)
    public ApiResponse<List<EmployeeCreationRequestResponse>> mine() {
        return ApiResponse.success(requestService.listMine(accessGuard.currentUserId()).stream()
                .filter(this::isVisibleToBranchManager)
                .toList());
    }

    // Admin sees any; a submitter only the ones they submitted.
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER', 'BRANCH_MANAGER', 'AGENT')")
    public ApiResponse<EmployeeCreationRequestResponse> getById(@PathVariable Long id, Authentication auth) {
        EmployeeCreationRequestResponse response = requestService.getById(id);

        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a ->
                a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_SUPER_ADMIN"));
        if (!isAdmin && (!response.requestedByUserId().equals(accessGuard.currentUserId())
                || !isVisibleToBranchManager(response))) {
            throw new AccessDeniedException("Not your request");
        }
        return ApiResponse.success(response);
    }

    // A Branch Manager who has moved leaves behind what they proposed for their old branch.
    private boolean isVisibleToBranchManager(EmployeeCreationRequestResponse response) {
        return !accessGuard.isBranchManager() || accessGuard.isSameBranch(response.branchId());
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Auditable(action = "EMPLOYEE_CREATION_APPROVED", entityType = "EmployeeCreationRequest")
    public ApiResponse<EmployeeCreationRequestResponse> approve(@PathVariable Long id) {
        return ApiResponse.success(
                "Request approved — employee created",
                requestService.approve(id, accessGuard.currentUserId()));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Auditable(action = "EMPLOYEE_CREATION_REJECTED", entityType = "EmployeeCreationRequest")
    public ApiResponse<EmployeeCreationRequestResponse> reject(
            @PathVariable Long id, @Valid @RequestBody EmployeeRequestRejection body
    ) {
        return ApiResponse.success(
                "Request rejected",
                requestService.reject(id, accessGuard.currentUserId(), body.reason()));
    }
}