// service/report/impl/ReportServiceImpl.java
package com.company.hrpayroll.service.report.impl;

import com.company.hrpayroll.dto.response.payroll.GeneratedDocument;
import com.company.hrpayroll.entity.attendance.Attendance;
import com.company.hrpayroll.entity.employee.Employee;
import com.company.hrpayroll.entity.payroll.Payslip;
import com.company.hrpayroll.repository.attendance.AttendanceRepository;
import com.company.hrpayroll.repository.employee.EmployeeRepository;
import com.company.hrpayroll.repository.payroll.PayslipRepository;
import com.company.hrpayroll.service.payroll.documents.support.PdfDocumentWriter;
import com.company.hrpayroll.service.report.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.List;

import static com.company.hrpayroll.service.payroll.documents.support.AmountFormatter.format;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final PayslipRepository payslipRepository;

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

    @Override
    public GeneratedDocument generatePayrollRegisterReport(Long payrollRunId) {
        List<Payslip> payslips = payslipRepository.findByPayrollRunId(payrollRunId);

        try (PdfDocumentWriter writer = new PdfDocumentWriter()) {
            writer.title("Payroll Register");
            writer.subtitle("Payroll Run ID: " + payrollRunId);
            writer.divider();

            for (Payslip payslip : payslips) {
                String employeeName = employeeRepository.findById(payslip.getEmployeeId())
                        .map(Employee::getFullName).orElse("Unknown");
                writer.row(employeeName, format(payslip.getNetPay()));
            }

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
}