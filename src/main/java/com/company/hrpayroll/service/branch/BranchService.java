// service/branch/BranchService.java
package com.company.hrpayroll.service.branch;

import com.company.hrpayroll.dto.request.branch.BranchRequest;
import com.company.hrpayroll.dto.response.branch.BranchResponse;

import java.util.List;

public interface BranchService {
    BranchResponse createBranch(BranchRequest request);
    BranchResponse updateBranch(Long id, BranchRequest request);
    BranchResponse getBranchById(Long id);
    List<BranchResponse> getAllBranches();
    List<BranchResponse> getBranchesByIds(List<Long> ids);
    void deactivateBranch(Long id);
    void activateBranch(Long id);
}