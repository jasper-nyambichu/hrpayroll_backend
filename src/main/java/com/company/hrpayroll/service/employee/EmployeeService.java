// service/employee/EmployeeService.java
package com.company.hrpayroll.service.employee;

import com.company.hrpayroll.dto.request.employee.EmployeeRequest;
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
    void terminateEmployee(Long id);
    void deleteEmployee(Long id);
}