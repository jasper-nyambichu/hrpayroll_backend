// service/department/DepartmentService.java
package com.company.hrpayroll.service.department;

import com.company.hrpayroll.dto.request.department.DepartmentRequest;
import com.company.hrpayroll.dto.response.department.DepartmentResponse;

import java.util.List;

public interface DepartmentService {
    DepartmentResponse createDepartment(DepartmentRequest request);
    DepartmentResponse updateDepartment(Long id, DepartmentRequest request);
    DepartmentResponse getDepartmentById(Long id);
    List<DepartmentResponse> getAllDepartments();
    List<DepartmentResponse> getDepartmentsByBranch(Long branchId);
    void deleteDepartment(Long id);
}