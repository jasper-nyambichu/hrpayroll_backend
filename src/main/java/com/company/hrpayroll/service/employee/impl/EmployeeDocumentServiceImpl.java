// service/employee/impl/EmployeeDocumentServiceImpl.java
package com.company.hrpayroll.service.employee.impl;

import com.company.hrpayroll.dto.response.employee.EmployeeDocumentResponse;
import com.company.hrpayroll.entity.employee.EmployeeDocument;
import com.company.hrpayroll.exception.ResourceNotFoundException;
import com.company.hrpayroll.mapper.employee.EmployeeDocumentMapper;
import com.company.hrpayroll.repository.employee.EmployeeDocumentRepository;
import com.company.hrpayroll.repository.employee.EmployeeRepository;
import com.company.hrpayroll.service.common.FileStorageService;
import com.company.hrpayroll.service.employee.EmployeeDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeDocumentServiceImpl implements EmployeeDocumentService {

    private final EmployeeDocumentRepository employeeDocumentRepository;
    private final EmployeeRepository employeeRepository;
    private final FileStorageService fileStorageService;
    private final EmployeeDocumentMapper employeeDocumentMapper;

    @Override
    public EmployeeDocumentResponse uploadDocument(Long employeeId, String documentType, MultipartFile file) {
        if (!employeeRepository.existsById(employeeId)) {
            throw new ResourceNotFoundException("Employee not found with id: " + employeeId);
        }

        String fileUrl = fileStorageService.upload(file, "employee-documents/" + employeeId);

        EmployeeDocument document = EmployeeDocument.builder()
                .employeeId(employeeId)
                .documentType(documentType)
                .fileUrl(fileUrl)
                .fileName(file.getOriginalFilename())
                .build();

        EmployeeDocument saved = employeeDocumentRepository.save(document);
        return employeeDocumentMapper.toEmployeeDocumentResponse(saved);
    }

    @Override
    public List<EmployeeDocumentResponse> getDocumentsByEmployee(Long employeeId) {
        return employeeDocumentRepository.findByEmployeeId(employeeId).stream()
                .map(employeeDocumentMapper::toEmployeeDocumentResponse)
                .toList();
    }

    @Override
    public void deleteDocument(Long documentId) {
        EmployeeDocument document = employeeDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + documentId));

        fileStorageService.delete(document.getFileUrl());
        employeeDocumentRepository.delete(document);
    }
}