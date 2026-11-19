// controller/offday/OffDayController.java
package com.company.hrpayroll.controller.offday;

import com.company.hrpayroll.audit.Auditable;
import com.company.hrpayroll.dto.request.offday.OffDayRequest;
import com.company.hrpayroll.dto.request.offday.OffDayUpdateRequest;
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

// The agent gives their employees days off (at most 2 single days a month). When the agent isn't available, the
// Branch Manager (own branch) or the Admin (anyone) can give, change or remove them too.
@RestController
@RequestMapping("/off-days")
@RequiredArgsConstructor
public class OffDayController {

    private static final String MANAGERS = "hasAnyRole('SUPER_ADMIN', 'ADMIN', 'BRANCH_MANAGER', 'AGENT')";

    private final OffDayService offDayService;
    private final AccessGuard accessGuard;

    private OffDayService.Actor actor() {
        return new OffDayService.Actor(accessGuard.currentRole(), accessGuard.currentAgentId(), accessGuard.currentBranchId());
    }

    @PostMapping
    @PreAuthorize(MANAGERS)
    @Auditable(action = "OFF_DAY_GRANTED", entityType = "OffDay")
    public ApiResponse<List<OffDayResponse>> grant(@Valid @RequestBody OffDayRequest request) {
        List<OffDayResponse> saved = offDayService.grant(actor(), request);
        return ApiResponse.success(saved.size() == 1 ? "Off day saved" : "Off days saved", saved);
    }

    @PutMapping("/{id}")
    @PreAuthorize(MANAGERS)
    @Auditable(action = "OFF_DAY_CHANGED", entityType = "OffDay")
    public ApiResponse<OffDayResponse> update(@PathVariable Long id, @Valid @RequestBody OffDayUpdateRequest request) {
        return ApiResponse.success("Off day changed", offDayService.update(actor(), id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(MANAGERS)
    @Auditable(action = "OFF_DAY_REMOVED", entityType = "OffDay")
    public ApiResponse<Void> remove(@PathVariable Long id) {
        offDayService.remove(actor(), id);
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

    @GetMapping("/my-branch")
    @PreAuthorize("hasRole('BRANCH_MANAGER')")
    public ApiResponse<List<OffDayResponse>> getMyBranch(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ApiResponse.success(offDayService.getForBranch(accessGuard.currentBranchId(), from, to));
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
