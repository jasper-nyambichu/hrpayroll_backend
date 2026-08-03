// dto/response/department/DepartmentResponse.java
package com.company.hrpayroll.dto.response.department;

public record DepartmentResponse(
        Long id,
        String name,
        Long branchId,
        String branchName,
        Long headOfDepartmentId,
        String headOfDepartmentName
) {}