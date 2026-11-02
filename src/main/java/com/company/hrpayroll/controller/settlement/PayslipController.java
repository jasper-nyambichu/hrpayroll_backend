// controller/settlement/PayslipController.java
package com.company.hrpayroll.controller.settlement;

import com.company.hrpayroll.dto.response.ApiResponse;
import com.company.hrpayroll.dto.response.common.GeneratedDocument;
import com.company.hrpayroll.dto.response.settlement.PayslipResponse;
import com.company.hrpayroll.security.AccessGuard;
import com.company.hrpayroll.service.settlement.PayslipService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Admin sees every payslip, a Branch Manager those of their own branch, an Agent those of the
// employees they were paid for: each can check what was due against what was recorded as paid.
@RestController
@RequestMapping("/payslips")
@RequiredArgsConstructor
public class PayslipController {

    private final PayslipService payslipService;
    private final AccessGuard accessGuard;

    private static final String CAN_READ_ONE = "hasAnyRole('SUPER_ADMIN', 'ADMIN') "
            + "or (hasRole('BRANCH_MANAGER') and @accessGuard.isPayslipInMyBranch(#payslipId)) "
            + "or (hasRole('AGENT') and @accessGuard.isPayslipOfMyAgent(#payslipId))";

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ApiResponse<List<PayslipResponse>> getAll() {
        return ApiResponse.success(payslipService.getAll());
    }

    @GetMapping("/my-branch")
    @PreAuthorize("hasRole('BRANCH_MANAGER')")
    public ApiResponse<List<PayslipResponse>> getMyBranch() {
        return ApiResponse.success(payslipService.getForBranch(accessGuard.currentBranchId()));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('AGENT')")
    public ApiResponse<List<PayslipResponse>> getMine() {
        return ApiResponse.success(payslipService.getForAgent(accessGuard.currentAgentId()));
    }

    @GetMapping("/{payslipId}")
    @PreAuthorize(CAN_READ_ONE)
    public ApiResponse<PayslipResponse> getOne(@PathVariable Long payslipId) {
        return ApiResponse.success(payslipService.getById(payslipId));
    }

    @GetMapping("/{payslipId}/pdf")
    @PreAuthorize(CAN_READ_ONE)
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long payslipId) {
        GeneratedDocument document = payslipService.getPdf(payslipId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDispositionFormData("attachment", document.fileName());

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(document.content());
    }
}
