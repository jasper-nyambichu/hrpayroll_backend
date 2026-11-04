// service/report/impl/ReportServiceImpl.java
package com.company.hrpayroll.service.report.impl;

import com.company.hrpayroll.dto.response.common.GeneratedDocument;
import com.company.hrpayroll.entity.attendance.Attendance;
import com.company.hrpayroll.entity.employee.Employee;
import com.company.hrpayroll.exception.ResourceNotFoundException;
import com.company.hrpayroll.repository.attendance.AttendanceRepository;
import com.company.hrpayroll.repository.employee.EmployeeRepository;
import com.company.hrpayroll.service.common.pdf.DateLabel;
import com.company.hrpayroll.service.company.CompanyService;
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
    private final CompanyService companyService;

    private static final float[] ATTENDANCE_COLUMNS = {245f, 130f, 120f};
    private static final float[] EMPLOYEE_COLUMNS = {245f, 100f, 150f};

    @Override
    public GeneratedDocument generateAttendanceReport(Long branchId, LocalDate start, LocalDate end) {
        List<Attendance> records = attendanceRepository.findByBranchIdAndAttendanceDateBetween(branchId, start, end);

        try (PdfDocumentWriter writer = new PdfDocumentWriter()) {
            writer.documentHeader(
                    "Attendance Report",
                    List.of(
                            new PdfDocumentWriter.Field("Period", DateLabel.range(start, end)),
                            new PdfDocumentWriter.Field("Branch ID", String.valueOf(branchId))),
                    companyService.getLetterhead(),
                    List.of());
            writer.tableRow(ATTENDANCE_COLUMNS, List.of("Employee", "Date", "Status"), true);

            for (Attendance record : records) {
                String employeeName = employeeRepository.findById(record.getEmployeeId())
                        .map(Employee::getFullName).orElse("Unknown");
                writer.tableRow(ATTENDANCE_COLUMNS, List.of(employeeName, DateLabel.format(record.getAttendanceDate()), String.valueOf(record.getStatus())), false);
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
            writer.documentHeader("Employee List", List.of(), companyService.getLetterhead(), List.of());
            writer.tableRow(EMPLOYEE_COLUMNS, List.of("Name", "Code", "Position"), true);

            for (Employee employee : employees) {
                writer.tableRow(EMPLOYEE_COLUMNS, List.of(employee.getFullName(), employee.getEmployeeNumber(), employee.getPosition() != null ? employee.getPosition() : "-"), false);
            }

            byte[] content = writer.build();
            return new GeneratedDocument("employee-list.pdf", "application/pdf", content);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to generate employee list report", e);
        }
    }

}