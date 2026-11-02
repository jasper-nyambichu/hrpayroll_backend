// service/employee/EmployeeExportService.java
package com.company.hrpayroll.service.employee;

import com.company.hrpayroll.dto.response.common.GeneratedDocument;

public interface EmployeeExportService {

    enum Format { PDF, CSV }

    // branchId and agentId narrow the list when set. The caller decides them from who is signed in,
    // never from the request alone, and decides whether this viewer may see basic salary.
    GeneratedDocument export(Format format, Long branchId, Long agentId, boolean includeSalary);
}
