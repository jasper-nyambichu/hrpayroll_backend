// mapper/employee/EmployeeDocumentMapper.java
package com.company.hrpayroll.mapper.employee;

import com.company.hrpayroll.dto.response.employee.EmployeeDocumentResponse;
import com.company.hrpayroll.entity.employee.EmployeeDocument;
import org.springframework.stereotype.Service;

@Service
public class EmployeeDocumentMapper {

    public EmployeeDocumentResponse toEmployeeDocumentResponse(EmployeeDocument document) {
        return new EmployeeDocumentResponse(
                document.getId(),
                document.getEmployeeId(),
                document.getDocumentType(),
                document.getFileUrl(),
                document.getFileName(),
                document.getCreatedAt()
        );
    }
}