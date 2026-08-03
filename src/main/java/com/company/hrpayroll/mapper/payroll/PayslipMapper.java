// mapper/payroll/PayslipMapper.java
package com.company.hrpayroll.mapper.payroll;

import com.company.hrpayroll.dto.response.payroll.PayslipResponse;
import com.company.hrpayroll.entity.payroll.Payslip;
import org.springframework.stereotype.Service;

@Service
public class PayslipMapper {

    public PayslipResponse toPayslipResponse(Payslip payslip, String employeeName) {
        return new PayslipResponse(
                payslip.getId(),
                payslip.getPayrollRunId(),
                payslip.getEmployeeId(),
                employeeName,
                payslip.getBasicSalary(),
                payslip.getHouseAllowance(),
                payslip.getTransportAllowance(),
                payslip.getOvertimePay(),
                payslip.getBonus(),
                payslip.getOtherAllowances(),
                payslip.getGrossPay(),
                payslip.getShifDeduction(),
                payslip.getNssfDeduction(),
                payslip.getHousingLevyDeduction(),
                payslip.getPayeDeduction(),
                payslip.getNitaDeduction(),
                payslip.getLoanDeduction(),
                payslip.getOtherDeductions(),
                payslip.getNetPay()
        );
    }
}