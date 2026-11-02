// service/report/impl/ReportServiceImpl.java
package com.company.hrpayroll.service.report.impl;

import com.company.hrpayroll.dto.response.common.GeneratedDocument;
import com.company.hrpayroll.entity.attendance.Attendance;
import com.company.hrpayroll.entity.employee.Employee;
import com.company.hrpayroll.exception.ResourceNotFoundException;
import com.company.hrpayroll.repository.attendance.AttendanceRepository;
import com.company.hrpayroll.repository.employee.EmployeeRepository;
import com.company.hrpayroll.service.common.pdf.PdfDocumentWriter;
import com.company.hrpayroll.service.report.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;

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