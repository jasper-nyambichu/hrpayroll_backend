// controller/offday/OffDayController.java
package com.company.hrpayroll.controller.offday;

import com.company.hrpayroll.audit.Auditable;
import com.company.hrpayroll.dto.request.offday.OffDayRequest;
import com.company.hrpayroll.dto.response.ApiResponse;
import com.company.hrpayroll.dto.response.offday.OffDayResponse;
import com.company.hrpayroll.security.AccessGuard;
import com.company.hrpayroll.service.offday.OffDayService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

// The agent is the one who gives employees time off. The Admin only reads it.
@RestController
@RequestMapping("/off-days")
@RequiredArgsConstructor
public class OffDayController {

    private final OffDayService offDayService;
    private final AccessGuard accessGuard;

    @PostMapping
    @PreAuthorize("hasRole('AGENT')")
    @Auditable(action = "OFF_DAY_GRANTED", entityType = "OffDay")
    public ApiResponse<OffDayResponse> grant(@Valid @RequestBody OffDayRequest request) {
        return ApiResponse.success("Off day saved", offDayService.grant(accessGuard.currentAgentId(), request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('AGENT')")
    @Auditable(action = "OFF_DAY_REMOVED", entityType = "OffDay")
    public ApiResponse<Void> remove(@PathVariable Long id) {
        offDayService.remove(accessGuard.currentAgentId(), id);
        return ApiResponse.success("Off day removed", null);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ApiResponse<List<OffDayResponse>> getAll(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ApiResponse.success(offDayService.getAll(from, to));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('AGENT')")
    public ApiResponse<List<OffDayResponse>> getMine(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ApiResponse.success(offDayService.getForAgent(accessGuard.currentAgentId(), from, to));
    }
}
