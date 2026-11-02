// service/branch/impl/BranchServiceImpl.java
package com.company.hrpayroll.service.branch.impl;

import com.company.hrpayroll.dto.request.branch.BranchRequest;
import com.company.hrpayroll.dto.response.branch.BranchResponse;
import com.company.hrpayroll.entity.auth.Role;
import com.company.hrpayroll.entity.auth.User;
import com.company.hrpayroll.entity.branch.Branch;
import com.company.hrpayroll.exception.DuplicateResourceException;
import com.company.hrpayroll.exception.InvalidOperationException;
import com.company.hrpayroll.exception.ResourceNotFoundException;
import com.company.hrpayroll.mapper.branch.BranchMapper;
import com.company.hrpayroll.repository.attendance.BiometricDeviceRepository;
import com.company.hrpayroll.repository.auth.UserRepository;
import com.company.hrpayroll.repository.branch.BranchRepository;
import com.company.hrpayroll.repository.employee.EmployeeRepository;
import com.company.hrpayroll.service.branch.BranchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BranchServiceImpl implements BranchService {

    private static final String DELETED = "DELETED";

    private final BranchRepository branchRepository;
    private final BiometricDeviceRepository biometricDeviceRepository;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final BranchMapper branchMapper;

    @Override
    @Transactional
    public BranchResponse createBranch(BranchRequest request) {
        BranchRequest normalized = normalize(request);

        validateManager(normalized.managerId(), null);

        Branch saved = branchRepository.save(branchMapper.toBranch(normalized, nextCode()));
        linkManager(saved, null);
        return toResponseWithManagerName(saved);
    }

    @Override
    @Transactional
    public BranchResponse updateBranch(Long id, BranchRequest request) {
        Branch branch = findBranchOrThrow(id);
        BranchRequest normalized = normalize(request);

        validateManager(normalized.managerId(), id);

        Long previousManagerId = branch.getManagerId();
        branchMapper.updateBranchFromRequest(branch, normalized);
        Branch saved = branchRepository.save(branch);
        linkManager(saved, previousManagerId);
        return toResponseWithManagerName(saved);
    }

    @Override
    public BranchResponse getBranchById(Long id) {
        return toResponseWithManagerName(findBranchOrThrow(id));
    }

    @Override
    public List<BranchResponse> getAllBranches() {
        return branchRepository.findByStatusNotOrderByCodeAsc(DELETED).stream()
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

        long activeEmployees = employeeRepository.findByBranchId(id).stream()
                .filter(employee -> "ACTIVE".equals(employee.getEmploymentStatus()))
                .count();
        if (activeEmployees > 0) {
            throw new InvalidOperationException(
                    "This branch still has " + activeEmployees + " active employee(s). Move or terminate them first.");
        }

        branch.setStatus("INACTIVE");
        branchRepository.save(branch);
    }

    // Refused while anyone still works here. The branch then vanishes from every list and its code is
    // freed, but the row stays so past damages, days worked and terminated staff keep the old name.
    @Override
    @Transactional
    public void deleteBranch(Long id) {
        Branch branch = findBranchOrThrow(id);

        long employees = employeeRepository.findByBranchId(id).stream()
                .filter(employee -> !"TERMINATED".equals(employee.getEmploymentStatus()))
                .count();
        if (employees > 0) {
            throw new InvalidOperationException(
                    "This branch still has " + employees + " employee(s). Move or delete them first.");
        }

        long terminals = biometricDeviceRepository.findByBranchId(id).stream()
                .filter(device -> "ACTIVE".equals(device.getStatus()))
                .count();
        if (terminals > 0) {
            throw new InvalidOperationException(
                    "This branch still has " + terminals + " active terminal(s). Deactivate them on the Devices page first.");
        }

        userRepository.findByBranchId(id).forEach(user -> {
            user.setBranchId(null);
            userRepository.save(user);
        });

        branch.setStatus(DELETED);
        branch.setCode("DEL-" + id);
        branch.setManagerId(null);
        branchRepository.save(branch);

        branchRepository.clearCodes();
        branchRepository.assignCodesInOrder();
    }

    @Override
    public void activateBranch(Long id) {
        Branch branch = findBranchOrThrow(id);
        branch.setStatus("ACTIVE");
        branchRepository.save(branch);
    }

    // The next index code. Deleting closes the gaps, so this is normally the count + 1.
    private String nextCode() {
        long position = branchRepository.countByStatusNot(DELETED) + 1;
        String candidate = String.format("%03d", position);

        while (branchRepository.existsByCode(candidate)) {
            position++;
            candidate = String.format("%03d", position);
        }

        return candidate;
    }

    private BranchRequest normalize(BranchRequest request) {
        return new BranchRequest(
                request.name().trim(),
                request.address(),
                request.phone(),
                request.email(),
                request.managerId()
        );
    }

    // The manager must be an active Branch Manager login who is not already running
    // another branch.
    private void validateManager(Long managerId, Long thisBranchId) {
        if (managerId == null) {
            return;
        }

        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException("Manager user not found with id: " + managerId));

        if (manager.getRole() != Role.BRANCH_MANAGER) {
            throw new InvalidOperationException("The selected user is not a Branch Manager");
        }
        if (!manager.isActive()) {
            throw new InvalidOperationException("The selected Branch Manager account is inactive");
        }

        boolean managesAnotherBranch = branchRepository.findByManagerId(managerId).stream()
                .anyMatch(other -> !other.getId().equals(thisBranchId));
        if (managesAnotherBranch) {
            throw new InvalidOperationException("The selected Branch Manager already manages another branch");
        }
    }

    // Branch-scoped access (AccessGuard.isSameBranch) reads User.branchId, so
    // assigning a manager must also point their login at this branch — and release
    // the previous manager's.
    private void linkManager(Branch saved, Long previousManagerId) {
        if (previousManagerId != null && !previousManagerId.equals(saved.getManagerId())) {
            userRepository.findById(previousManagerId).ifPresent(previous -> {
                if (saved.getId().equals(previous.getBranchId())) {
                    previous.setBranchId(null);
                    userRepository.save(previous);
                }
            });
        }

        if (saved.getManagerId() != null) {
            userRepository.findById(saved.getManagerId()).ifPresent(manager -> {
                manager.setBranchId(saved.getId());
                userRepository.save(manager);
            });
        }
    }

    private Branch findBranchOrThrow(Long id) {
        return branchRepository.findById(id)
                .filter(branch -> !DELETED.equals(branch.getStatus()))
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