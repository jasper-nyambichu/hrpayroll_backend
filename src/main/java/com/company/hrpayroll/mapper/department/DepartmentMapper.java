// mapper/department/DepartmentMapper.java
package com.company.hrpayroll.mapper.department;

import com.company.hrpayroll.dto.request.department.DepartmentRequest;
import com.company.hrpayroll.dto.response.department.DepartmentResponse;
import com.company.hrpayroll.entity.department.Department;
import org.springframework.stereotype.Service;

@Service
public class DepartmentMapper {

    public Department toDepartment(DepartmentRequest dto) {
        return Department.builder()
                .name(dto.name())
                .branchId(dto.branchId())
                .headOfDepartmentId(dto.headOfDepartmentId())
                .build();
    }

    public void updateDepartmentFromRequest(Department department, DepartmentRequest dto) {
        department.setName(dto.name());
        department.setBranchId(dto.branchId());
        department.setHeadOfDepartmentId(dto.headOfDepartmentId());
    }

    public DepartmentResponse toDepartmentResponse(Department department, String branchName, String headName) {
        return new DepartmentResponse(
                department.getId(),
                department.getName(),
                department.getBranchId(),
                branchName,
                department.getHeadOfDepartmentId(),
                headName
        );
    }
}