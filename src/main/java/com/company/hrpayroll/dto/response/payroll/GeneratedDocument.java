// dto/response/payroll/GeneratedDocument.java
package com.company.hrpayroll.dto.response.payroll;

public record GeneratedDocument(
        String fileName,
        String contentType,
        byte[] content
) {}