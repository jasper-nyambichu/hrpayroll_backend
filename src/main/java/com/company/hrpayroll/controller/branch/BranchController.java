// controller/branch/BranchController.java
package com.company.hrpayroll.controller.branch;

import com.company.hrpayroll.audit.Auditable;
import com.company.hrpayroll.dto.request.branch.BranchRequest;
import com.company.hrpayroll.dto.response.ApiResponse;
import com.company.hrpayroll.dto.response.branch.BranchResponse;
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
    private final com.company.hrpayroll.security.AccessGuard accessGuard;

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

    // Super Admin/Admin see any branch by ID; a Branch Manager can only
    // fetch their own — this is AccessGuard.isSameBranch() doing exactly
    // what it was built for back in the security phase.
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or @accessGuard.isSameBranch(#id)")
    public ApiResponse<BranchResponse> getBranchById(@PathVariable Long id) {
        return ApiResponse.success(branchService.getBranchById(id));
    }

    // Convenience endpoint so the Branch Manager frontend doesn't need to
    // already know its own branch ID before it can call anything.
     // add to constructor via @RequiredArgsConstructor

    @GetMapping("/my-branch")
    @PreAuthorize("hasRole('BRANCH_MANAGER')")
    public ApiResponse<BranchResponse> getMyBranch() {
        return ApiResponse.success(branchService.getBranchById(accessGuard.currentBranchId()));
    }
    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ApiResponse<List<BranchResponse>> getAllBranches() {
        return ApiResponse.success(branchService.getAllBranches());
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Auditable(action = "BRANCH_DEACTIVATED", entityType = "Branch")
    public ApiResponse<Void> deactivateBranch(@PathVariable Long id) {
        branchService.deactivateBranch(id);
        return ApiResponse.success("Branch deactivated", null);
    }

    // Small helper so /my-branch doesn't need AccessGuard injected just for
    // one call — reads the same way @accessGuard.currentBranchId() would.
    private Long accessGuardCurrentBranchId() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        var user = (com.company.hrpayroll.entity.auth.User) auth.getPrincipal();
        return user.getBranchId();
    }
}