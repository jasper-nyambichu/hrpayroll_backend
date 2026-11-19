// service/offday/OffDayService.java
package com.company.hrpayroll.service.offday;

import com.company.hrpayroll.dto.request.offday.OffDayRequest;
import com.company.hrpayroll.dto.request.offday.OffDayUpdateRequest;
import com.company.hrpayroll.dto.response.offday.OffDayResponse;
import com.company.hrpayroll.entity.auth.Role;

import java.time.LocalDate;
import java.util.List;

public interface OffDayService {

    /** At most this many off days per employee in one calendar month. */
    int MAX_PER_MONTH = 2;

    /**
     * Who is giving the day off: an agent (their own employees), a Branch Manager (their branch's employees, when
     * the agent isn't available) or the Admin (anyone).
     */
    record Actor(Role role, Long agentId, Long branchId) {}

    List<OffDayResponse> grant(Actor actor, OffDayRequest request);
    OffDayResponse update(Actor actor, Long offDayId, OffDayUpdateRequest request);
    void remove(Actor actor, Long offDayId);

    List<OffDayResponse> getAll(LocalDate from, LocalDate to);
    List<OffDayResponse> getForAgent(Long agentId, LocalDate from, LocalDate to);
    List<OffDayResponse> getForBranch(Long branchId, LocalDate from, LocalDate to);
}
