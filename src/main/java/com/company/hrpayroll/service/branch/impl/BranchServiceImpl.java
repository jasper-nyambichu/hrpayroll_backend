// service/branch/impl/BranchServiceImpl.java
package com.company.hrpayroll.service.branch.impl;

import com.company.hrpayroll.dto.request.branch.BranchRequest;
import com.company.hrpayroll.dto.response.branch.BranchResponse;
import com.company.hrpayroll.entity.branch.Branch;
import com.company.hrpayroll.exception.DuplicateResourceException;
import com.company.hrpayroll.exception.ResourceNotFoundException;
import com.company.hrpayroll.mapper.branch.BranchMapper;
import com.company.hrpayroll.repository.auth.UserRepository;
import com.company.hrpayroll.repository.branch.BranchRepository;
import com.company.hrpayroll.service.branch.BranchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BranchServiceImpl implements BranchService {

    private final BranchRepository branchRepository;
    private final UserRepository userRepository;
    private final BranchMapper branchMapper;

    @Override
    public BranchResponse createBranch(BranchRequest request) {
        if (branchRepository.existsByCode(request.code())) {
            throw new DuplicateResourceException("Branch code '" + request.code() + "' is already in use");
        }

        Branch branch = branchMapper.toBranch(request);
        Branch saved = branchRepository.save(branch);
        return toResponseWithManagerName(saved);
    }

    @Override
    public BranchResponse updateBranch(Long id, BranchRequest request) {
        Branch branch = findBranchOrThrow(id);

        // Only re-check uniqueness if the code is actually changing.
        if (!branch.getCode().equals(request.code()) && branchRepository.existsByCode(request.code())) {
            throw new DuplicateResourceException("Branch code '" + request.code() + "' is already in use");
        }

        branchMapper.updateBranchFromRequest(branch, request);
        Branch saved = branchRepository.save(branch);
        return toResponseWithManagerName(saved);
    }

    @Override
    public BranchResponse getBranchById(Long id) {
        return toResponseWithManagerName(findBranchOrThrow(id));
    }

    @Override
    public List<BranchResponse> getAllBranches() {
        return branchRepository.findAll().stream()
                .map(this::toResponseWithManagerName)
                .toList();
    }

    @Override
    public List<BranchResponse> getBranchesByIds(List<Long> ids) {
        return branchRepository.findByIdIn(ids).stream()
                .map(this::toResponseWithManagerName)
                .toList();
    }

    @Override
    public void deactivateBranch(Long id) {
        Branch branch = findBranchOrThrow(id);
        branch.setStatus("INACTIVE");
        branchRepository.save(branch);
    }

    private Branch findBranchOrThrow(Long id) {
        return branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found with id: " + id));
    }

    private BranchResponse toResponseWithManagerName(Branch branch) {
        String managerName = branch.getManagerId() != null
                ? userRepository.findById(branch.getManagerId())
                .map(u -> u.getDisplayName())
                .orElse(null)
                : null;
        return branchMapper.toBranchResponse(branch, managerName);
    }
}