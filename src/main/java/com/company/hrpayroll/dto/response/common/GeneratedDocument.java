// dto/response/common/GeneratedDocument.java
package com.company.hrpayroll.dto.response.common;

public record GeneratedDocument(
        String fileName,
        String contentType,
        byte[] content
) {}