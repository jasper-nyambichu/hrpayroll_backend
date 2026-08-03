// service/employee/EmployeeDocumentService.java
package com.company.hrpayroll.service.employee;

import com.company.hrpayroll.dto.response.employee.EmployeeDocumentResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface EmployeeDocumentService {
    EmployeeDocumentResponse uploadDocument(Long employeeId, String documentType, MultipartFile file);
    List<EmployeeDocumentResponse> getDocumentsByEmployee(Long employeeId);
    void deleteDocument(Long documentId);
}