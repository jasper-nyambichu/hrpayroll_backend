// service/payroll/engine/PayrollCalculationService.java
package com.company.hrpayroll.service.payroll.engine;

import com.company.hrpayroll.entity.employee.Employee;
import com.company.hrpayroll.entity.payroll.PayrollRun;
import com.company.hrpayroll.entity.payroll.Payslip;

import java.util.List;

public interface PayrollCalculationService {
    // The one calculation both the employee side and the agent side use. Pay = days worked x the
    // daily rate; SHIF and NSSF (as the Admin set them) are added on top; loans and approved
    // damages come off, within the deduction ceiling. Builds in memory — does NOT persist it, and
    // does not mark loans or damages as taken: the caller does that from the returned calculation.
    PayslipCalculation calculate(Employee employee, PayrollRun run, int daysWorked);

    // Looks up the days the Back Office locked for the run's exact period; fails if there are none.
    Payslip calculateForEmployee(Employee employee, PayrollRun run);

    List<Payslip> calculateForEmployees(List<Employee> employees, PayrollRun run);
}