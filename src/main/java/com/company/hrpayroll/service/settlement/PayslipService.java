// service/settlement/PayslipService.java
package com.company.hrpayroll.service.settlement;

import com.company.hrpayroll.dto.response.common.GeneratedDocument;
import com.company.hrpayroll.dto.response.settlement.PayslipResponse;

import java.util.List;

public interface PayslipService {
    List<PayslipResponse> getAll();
    List<PayslipResponse> getForBranch(Long branchId);
    List<PayslipResponse> getForAgent(Long agentId);
    PayslipResponse getById(Long payslipId);
    GeneratedDocument getPdf(Long payslipId);
}
