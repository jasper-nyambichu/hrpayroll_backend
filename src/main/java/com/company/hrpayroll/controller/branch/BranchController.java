// controller/branch/BranchController.java
package com.company.hrpayroll.controller.branch;

import com.company.hrpayroll.audit.Auditable;
import com.company.hrpayroll.dto.request.branch.BranchRequest;
import com.company.hrpayroll.dto.response.ApiResponse;
import com.company.hrpayroll.dto.response.branch.BranchResponse;
import com.company.hrpayroll.security.AccessGuard;
import com.company.hrpayroll.service.branch.BranchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/branches")
@RequiredArgsConstructor
public class BranchController {

    private final BranchService branchService;
    private final AccessGuard accessGuard;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Auditable(action = "BRANCH_CREATED", entityType = "Branch")
    public ApiResponse<BranchResponse> createBranch(@Valid @RequestBody BranchRequest request) {
        return ApiResponse.success("Branch created", branchService.createBranch(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Auditable(action = "BRANCH_UPDATED", entityType = "Branch")
    public ApiResponse<BranchResponse> updateBranch(
            @PathVariable Long id, @Valid @RequestBody BranchRequest request
    ) {
        return ApiResponse.success("Branch updated", branchService.updateBranch(id, request));
    }

    // Super Admin/Admin see any branch by ID, as do HR and the read-only finance roles;
    // a Branch Manager can only fetch their own — AccessGuard.isSameBranch() at work.
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER', 'FINANCE_OFFICER', 'AUDITOR') or @accessGuard.isSameBranch(#id)")
    public ApiResponse<BranchResponse> getBranchById(@PathVariable Long id) {
        return ApiResponse.success(branchService.getBranchById(id));
    }

    // Convenience endpoint so the Branch Manager frontend doesn't need to
    // already know its own branch ID before it can call anything.
    @GetMapping("/my-branch")
    @PreAuthorize("hasRole('BRANCH_MANAGER')")
    public ApiResponse<BranchResponse> getMyBranch() {
        return ApiResponse.success(branchService.getBranchById(accessGuard.currentBranchId()));
    }

    // HR_MANAGER needs this list to choose a branch on the Add Employee form; PAYROLL_OFFICER to
    // choose the scope of a payroll run; FINANCE_OFFICER and AUDITOR read it; BACK_OFFICE filters
    // attendance and devices by branch; AGENT chooses where to place the staff they add.
    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER', 'PAYROLL_OFFICER', 'FINANCE_OFFICER', 'AUDITOR', 'BACK_OFFICE', 'AGENT')")
    public ApiResponse<List<BranchResponse>> getAllBranches() {
        return ApiResponse.success(branchService.getAllBranches());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Auditable(action = "BRANCH_DELETED", entityType = "Branch")
    public ApiResponse<Void> deleteBranch(@PathVariable Long id) {
        branchService.deleteBranch(id);
        return ApiResponse.success("Branch deleted", null);
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Auditable(action = "BRANCH_DEACTIVATED", entityType = "Branch")
    public ApiResponse<Void> deactivateBranch(@PathVariable Long id) {
        branchService.deactivateBranch(id);
        return ApiResponse.success("Branch deactivated", null);
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Auditable(action = "BRANCH_ACTIVATED", entityType = "Branch")
    public ApiResponse<Void> activateBranch(@PathVariable Long id) {
        branchService.activateBranch(id);
        return ApiResponse.success("Branch activated", null);
    }
}