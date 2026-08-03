// mapper/branch/BranchMapper.java
package com.company.hrpayroll.mapper.branch;

import com.company.hrpayroll.dto.request.branch.BranchRequest;
import com.company.hrpayroll.dto.response.branch.BranchResponse;
import com.company.hrpayroll.entity.branch.Branch;
import org.springframework.stereotype.Service;

@Service
public class BranchMapper {

    public Branch toBranch(BranchRequest dto) {
        return Branch.builder()
                .name(dto.name())
                .code(dto.code())
                .address(dto.address())
                .phone(dto.phone())
                .email(dto.email())
                .managerId(dto.managerId())
                .build();
    }

    public void updateBranchFromRequest(Branch branch, BranchRequest dto) {
        branch.setName(dto.name());
        branch.setCode(dto.code());
        branch.setAddress(dto.address());
        branch.setPhone(dto.phone());
        branch.setEmail(dto.email());
        branch.setManagerId(dto.managerId());
    }

    public BranchResponse toBranchResponse(Branch branch, String managerName) {
        return new BranchResponse(
                branch.getId(),
                branch.getName(),
                branch.getCode(),
                branch.getAddress(),
                branch.getPhone(),
                branch.getEmail(),
                branch.getManagerId(),
                managerName,
                branch.getStatus()
        );
    }
}