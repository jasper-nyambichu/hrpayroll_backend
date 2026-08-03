// service/payroll/engine/PayrollCalculationService.java
package com.company.hrpayroll.service.payroll.engine;

import com.company.hrpayroll.entity.employee.Employee;
import com.company.hrpayroll.entity.payroll.PayrollRun;
import com.company.hrpayroll.entity.payroll.Payslip;

import java.util.List;

public interface PayrollCalculationService {
    // Builds a Payslip in memory — does NOT persist it. Persistence and
    // orchestration (creating the PayrollRun, saving all payslips, updating
    // run totals) belongs to payroll/batch, coming in the next phase.
    Payslip calculateForEmployee(Employee employee, PayrollRun run);

    List<Payslip> calculateForEmployees(List<Employee> employees, PayrollRun run);
}