// mapper/payroll/PayslipMapper.java
package com.company.hrpayroll.mapper.payroll;

import com.company.hrpayroll.dto.response.payroll.PayslipResponse;
import com.company.hrpayroll.entity.employee.Employee;
import com.company.hrpayroll.entity.payroll.PayrollRun;
import com.company.hrpayroll.entity.payroll.Payslip;
import org.springframework.stereotype.Service;

@Service
public class PayslipMapper {

    public PayslipResponse toPayslipResponse(Payslip payslip, Employee employee, PayrollRun run) {
        return new PayslipResponse(
                payslip.getId(),
                payslip.getPayrollRunId(),
                run.getPeriodStart(),
                run.getPeriodEnd(),
                payslip.getEmployeeId(),
                employee != null ? employee.getFullName() : null,
                employee != null ? employee.getEmployeeNumber() : null,
                payslip.getBasicSalary(),
                payslip.getHouseAllowance(),
                payslip.getTransportAllowance(),
                payslip.getOvertimePay(),
                payslip.getBonus(),
                payslip.getOtherAllowances(),
                payslip.getUnpaidLeaveDeduction(),
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