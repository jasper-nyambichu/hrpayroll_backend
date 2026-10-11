// service/employee/EmployeeCreationRequestService.java
package com.company.hrpayroll.service.employee;

import com.company.hrpayroll.dto.request.employee.EmployeeRequest;
import com.company.hrpayroll.dto.response.employee.EmployeeCreationRequestResponse;
import com.company.hrpayroll.entity.employee.EmployeeRequestStatus;

import java.util.List;

public interface EmployeeCreationRequestService {
    EmployeeCreationRequestResponse submit(EmployeeRequest request, Long requestedByUserId);
    List<EmployeeCreationRequestResponse> list(EmployeeRequestStatus statusOrNull);
    List<EmployeeCreationRequestResponse> listMine(Long userId);
    EmployeeCreationRequestResponse getById(Long id);
    EmployeeCreationRequestResponse approve(Long id, Long reviewerUserId);
    EmployeeCreationRequestResponse reject(Long id, Long reviewerUserId, String reason);
}