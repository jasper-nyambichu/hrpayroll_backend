// controller/report/ReportController.java
package com.company.hrpayroll.controller.report;

import com.company.hrpayroll.dto.response.common.GeneratedDocument;
import com.company.hrpayroll.security.AccessGuard;
import com.company.hrpayroll.service.report.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final AccessGuard accessGuard;

    private static final String REPORT_ROLES =
            "hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER', 'PAYROLL_OFFICER', 'FINANCE_OFFICER', 'AUDITOR')";

    // Attendance is also the Back Office officer's report; payroll reports stay closed to that role.
    private static final String ATTENDANCE_REPORT_ROLES =
            "hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER', 'PAYROLL_OFFICER', 'FINANCE_OFFICER', 'AUDITOR', 'BACK_OFFICE')";

    // Staff with the broad report roles can pull any branch; a Branch
    // Manager is restricted to their own — same pattern as every other
    // branch-scoped read endpoint built so far.
    @GetMapping("/attendance/{branchId}")
    @PreAuthorize(ATTENDANCE_REPORT_ROLES + " or @accessGuard.isSameBranch(#branchId)")
    public ResponseEntity<byte[]> getAttendanceReport(
            @PathVariable Long branchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        return toPdfDownload(reportService.generateAttendanceReport(branchId, start, end));
    }

    // branchIds as a comma-separated query param keeps this a simple GET
    // (bookmarkable, cacheable) rather than needing a POST with a body just
    // to pass a list — reasonable for a read-only report endpoint.
    @GetMapping("/employee-list")
    @PreAuthorize(REPORT_ROLES)
    public ResponseEntity<byte[]> getEmployeeListReport(@RequestParam List<Long> branchIds) {
        return toPdfDownload(reportService.generateEmployeeListReport(branchIds));
    }

    private ResponseEntity<byte[]> toPdfDownload(GeneratedDocument document) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", document.fileName());

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(document.content());
    }
}