// service/employee/EmployeeService.java
package com.company.hrpayroll.service.employee;

import com.company.hrpayroll.dto.request.employee.EmployeeRequest;
import com.company.hrpayroll.dto.response.employee.EmployeeDirectoryResponse;
import com.company.hrpayroll.dto.response.employee.EmployeeResponse;
import com.company.hrpayroll.dto.response.employee.EmployeeSummaryResponse;

import java.util.List;

public interface EmployeeService {
    EmployeeResponse createEmployee(EmployeeRequest request);
    EmployeeResponse updateEmployee(Long id, EmployeeRequest request);
    EmployeeResponse getEmployeeById(Long id);
    List<EmployeeSummaryResponse> getEmployeesByBranch(Long branchId);
    List<EmployeeSummaryResponse> getEmployeesByAgent(Long agentId);
    List<EmployeeSummaryResponse> getEmployeesByDepartment(Long departmentId);

    // Admin: everyone in a branch.
    List<EmployeeDirectoryResponse> getBranchDirectory(Long branchId);

    // Agent: only their own employees in a branch.
    List<EmployeeDirectoryResponse> getBranchDirectoryForAgent(Long branchId, Long agentId);

    // Moves one employee into a company-wide department. Nothing else on the record changes.
    EmployeeResponse assignDepartment(Long id, Long departmentId);

    void terminateEmployee(Long id);
    void deleteEmployee(Long id);
}