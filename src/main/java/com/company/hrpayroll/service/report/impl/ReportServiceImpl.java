// service/report/impl/ReportServiceImpl.java
package com.company.hrpayroll.service.report.impl;

import com.company.hrpayroll.dto.response.payroll.GeneratedDocument;
import com.company.hrpayroll.entity.attendance.Attendance;
import com.company.hrpayroll.entity.employee.Employee;
import com.company.hrpayroll.entity.payroll.PayrollRun;
import com.company.hrpayroll.entity.payroll.Payslip;
import com.company.hrpayroll.exception.ResourceNotFoundException;
import com.company.hrpayroll.repository.attendance.AttendanceRepository;
import com.company.hrpayroll.repository.employee.EmployeeRepository;
import com.company.hrpayroll.repository.payroll.PayrollRunRepository;
import com.company.hrpayroll.repository.payroll.PayslipRepository;
import com.company.hrpayroll.service.payroll.documents.support.PdfDocumentWriter;
import com.company.hrpayroll.service.report.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;

import static com.company.hrpayroll.service.payroll.documents.support.AmountFormatter.format;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final PayslipRepository payslipRepository;
    private final PayrollRunRepository payrollRunRepository;

    @Override
    public GeneratedDocument generateAttendanceReport(Long branchId, LocalDate start, LocalDate end) {
        List<Attendance> records = attendanceRepository.findByBranchIdAndAttendanceDateBetween(branchId, start, end);

        try (PdfDocumentWriter writer = new PdfDocumentWriter()) {
            writer.title("Attendance Report");
            writer.subtitle("Branch ID: " + branchId + "   Period: " + start + " to " + end);
            writer.divider();

            for (Attendance record : records) {
                String employeeName = employeeRepository.findById(record.getEmployeeId())
                        .map(Employee::getFullName).orElse("Unknown");
                writer.row(employeeName + " (" + record.getAttendanceDate() + ")", record.getStatus());
            }

            byte[] content = writer.build();
            return new GeneratedDocument("attendance-report-" + branchId + ".pdf", "application/pdf", content);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to generate attendance report", e);
        }
    }

    // Per-employee gross / deductions / net, then run totals and the statutory
    // totals the finance team remits (SHIF, NSSF, Housing Levy, PAYE) plus the
    // employer-borne NITA levy.
    @Override
    public GeneratedDocument generatePayrollRegisterReport(Long payrollRunId) {
        PayrollRun run = payrollRunRepository.findById(payrollRunId)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll run not found with id: " + payrollRunId));
        List<Payslip> payslips = payslipRepository.findByPayrollRunId(payrollRunId);

        try (PdfDocumentWriter writer = new PdfDocumentWriter()) {
            writer.title("Payroll Register");
            writer.subtitle("Payroll Run #" + payrollRunId + "   Period: " + run.getPeriodStart() + " to " + run.getPeriodEnd());
            writer.subtitle("Employees: " + payslips.size());
            writer.divider();

            BigDecimal totalGross = BigDecimal.ZERO;
            BigDecimal totalNet = BigDecimal.ZERO;

            for (Payslip payslip : payslips) {
                Employee employee = employeeRepository.findById(payslip.getEmployeeId()).orElse(null);
                String label = employee != null
                        ? employee.getFullName() + " (" + employee.getEmployeeNumber() + ")"
                        : "Unknown (#" + payslip.getEmployeeId() + ")";

                BigDecimal deductions = payslip.getGrossPay().subtract(payslip.getNetPay());

                writer.row(
                        shorten(label, 34),
                        "Gross " + format(payslip.getGrossPay())
                                + "  Ded " + format(deductions)
                                + "  Net " + format(payslip.getNetPay())
                );

                totalGross = totalGross.add(payslip.getGrossPay());
                totalNet = totalNet.add(payslip.getNetPay());
            }

            writer.divider();
            writer.boldRow("Total Gross Pay", format(totalGross));
            writer.boldRow("Total Deductions", format(totalGross.subtract(totalNet)));
            writer.boldRow("Total Net Pay", format(totalNet));

            writer.sectionHeading("Statutory totals (to remit)");
            writer.row("SHIF", format(sum(payslips, Payslip::getShifDeduction)));
            writer.row("NSSF", format(sum(payslips, Payslip::getNssfDeduction)));
            writer.row("Housing Levy", format(sum(payslips, Payslip::getHousingLevyDeduction)));
            writer.row("PAYE", format(sum(payslips, Payslip::getPayeDeduction)));
            writer.row("Loan repayments", format(sum(payslips, Payslip::getLoanDeduction)));

            writer.sectionHeading("Employer cost");
            writer.row("NITA levy (not deducted from employees)", format(sum(payslips, Payslip::getNitaDeduction)));

            byte[] content = writer.build();
            return new GeneratedDocument("payroll-register-" + payrollRunId + ".pdf", "application/pdf", content);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to generate payroll register", e);
        }
    }

    @Override
    public GeneratedDocument generateEmployeeListReport(List<Long> branchIds) {
        List<Employee> employees = employeeRepository.findByBranchIdIn(branchIds);

        try (PdfDocumentWriter writer = new PdfDocumentWriter()) {
            writer.title("Employee List");
            writer.divider();

            for (Employee employee : employees) {
                writer.row(employee.getFullName() + " (" + employee.getEmployeeNumber() + ")", employee.getPosition());
            }

            byte[] content = writer.build();
            return new GeneratedDocument("employee-list.pdf", "application/pdf", content);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to generate employee list report", e);
        }
    }

    private BigDecimal sum(List<Payslip> payslips, Function<Payslip, BigDecimal> extractor) {
        return payslips.stream()
                .map(extractor)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Keeps a long name from running into the amounts printed on the right.
    private String shorten(String text, int maxLength) {
        return text.length() <= maxLength ? text : text.substring(0, maxLength - 1) + ".";
    }
}