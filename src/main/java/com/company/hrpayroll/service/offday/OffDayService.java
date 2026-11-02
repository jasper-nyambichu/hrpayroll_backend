// service/offday/OffDayService.java
package com.company.hrpayroll.service.offday;

import com.company.hrpayroll.dto.request.offday.OffDayRequest;
import com.company.hrpayroll.dto.response.offday.OffDayResponse;

import java.time.LocalDate;
import java.util.List;

public interface OffDayService {
    OffDayResponse grant(Long agentId, OffDayRequest request);
    void remove(Long agentId, Long offDayId);

    List<OffDayResponse> getAll(LocalDate from, LocalDate to);
    List<OffDayResponse> getForAgent(Long agentId, LocalDate from, LocalDate to);
}
