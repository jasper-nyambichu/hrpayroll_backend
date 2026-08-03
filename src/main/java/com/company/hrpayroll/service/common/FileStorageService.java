// service/common/FileStorageService.java
package com.company.hrpayroll.service.common;

import org.springframework.web.multipart.MultipartFile;

// Implemented once real object storage (S3-compatible) is wired up.
// Every module that stores a file — employee documents, passport photos,
// generated payroll PDFs — depends on this interface, not on S3 directly.
public interface FileStorageService {
    String upload(MultipartFile file, String folder);
    void delete(String fileUrl);
}