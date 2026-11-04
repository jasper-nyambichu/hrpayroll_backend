// controller/employee/EmployeeDocumentController.java
package com.company.hrpayroll.controller.employee;

import com.company.hrpayroll.audit.Auditable;
import com.company.hrpayroll.dto.response.ApiResponse;
import com.company.hrpayroll.dto.response.employee.EmployeeDocumentResponse;
import com.company.hrpayroll.service.employee.EmployeeDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/employees/{employeeId}/documents")
@RequiredArgsConstructor
public class EmployeeDocumentController {

    private final EmployeeDocumentService employeeDocumentService;

    // documentType is a plain request param here rather than part of a JSON
    // body, since this is a multipart/form-data upload, not a JSON request —
    // matches how file uploads work with Spring MVC's MultipartFile binding.
    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER')")
    @Auditable(action = "EMPLOYEE_DOCUMENT_UPLOADED", entityType = "EmployeeDocument")
    public ApiResponse<EmployeeDocumentResponse> uploadDocument(
            @PathVariable Long employeeId,
            @RequestParam String documentType,
            @RequestParam MultipartFile file
    ) {
        return ApiResponse.success(
                "Document uploaded",
                employeeDocumentService.uploadDocument(employeeId, documentType, file)
        );
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER', 'EMPLOYEE') " +
            "or (hasRole('BRANCH_MANAGER') and @accessGuard.isEmployeeInMyBranch(#employeeId))")
    public ApiResponse<List<EmployeeDocumentResponse>> getDocuments(@PathVariable Long employeeId) {
        return ApiResponse.success(employeeDocumentService.getDocumentsByEmployee(employeeId));
    }

    @DeleteMapping("/{documentId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER')")
    @Auditable(action = "EMPLOYEE_DOCUMENT_DELETED", entityType = "EmployeeDocument")
    public ApiResponse<Void> deleteDocument(@PathVariable Long employeeId, @PathVariable Long documentId) {
        employeeDocumentService.deleteDocument(documentId);
        return ApiResponse.success("Document deleted", null);
    }
}