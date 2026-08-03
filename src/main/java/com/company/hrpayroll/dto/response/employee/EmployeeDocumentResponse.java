// dto/response/employee/EmployeeDocumentResponse.java
package com.company.hrpayroll.dto.response.employee;

import java.time.LocalDateTime;

public record EmployeeDocumentResponse(
        Long id,
        Long employeeId,
        String documentType,
        String fileUrl,
        String fileName,
        LocalDateTime uploadedAt
) {}