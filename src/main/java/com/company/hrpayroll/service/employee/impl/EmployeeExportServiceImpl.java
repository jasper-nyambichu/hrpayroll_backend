// service/employee/impl/EmployeeExportServiceImpl.java
package com.company.hrpayroll.service.employee.impl;

import com.company.hrpayroll.config.BusinessClock;
import com.company.hrpayroll.dto.response.common.GeneratedDocument;
import com.company.hrpayroll.entity.agent.Agent;
import com.company.hrpayroll.entity.branch.Branch;
import com.company.hrpayroll.entity.department.Department;
import com.company.hrpayroll.entity.employee.Employee;
import com.company.hrpayroll.repository.agent.AgentRepository;
import com.company.hrpayroll.repository.branch.BranchRepository;
import com.company.hrpayroll.repository.department.DepartmentRepository;
import com.company.hrpayroll.repository.employee.EmployeeRepository;
import com.company.hrpayroll.service.common.pdf.PdfDocumentWriter;
import com.company.hrpayroll.service.company.CompanyService;
import com.company.hrpayroll.service.employee.EmployeeExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.company.hrpayroll.service.common.pdf.AmountFormatter.format;

@Service
@RequiredArgsConstructor
public class EmployeeExportServiceImpl implements EmployeeExportService {

    private static final float[] PDF_COLUMNS = {80f, 130f, 75f, 80f, 80f, 50f};
    private static final float[] PDF_COLUMNS_NO_SALARY = {85f, 150f, 80f, 90f, 90f};

    private static final Pattern PHONE_NUMBER = Pattern.compile("^\\+?[0-9 ]+$");

    private final EmployeeRepository employeeRepository;
    private final BranchRepository branchRepository;
    private final DepartmentRepository departmentRepository;
    private final AgentRepository agentRepository;
    private final CompanyService companyService;
    private final BusinessClock businessClock;

    @Override
    public GeneratedDocument export(Format format, Long branchId, Long agentId, boolean includeSalary) {
        List<Employee> employees = selectEmployees(branchId, agentId);

        Map<Long, String> branches = branchRepository.findAll().stream().collect(Collectors.toMap(Branch::getId, Branch::getName));
        Map<Long, String> departments = departmentRepository.findAll().stream().collect(Collectors.toMap(Department::getId, Department::getName));
        Map<Long, String> agents = agentRepository.findAll().stream().collect(Collectors.toMap(Agent::getId, Agent::getName));

        String baseName = "employee-list-" + businessClock.today();

        if (format == Format.CSV) {
            byte[] content = toCsv(employees, branches, departments, agents, includeSalary);
            return new GeneratedDocument(baseName + ".csv", "text/csv", content);
        }
        byte[] content = toPdf(employees, branches, departments, agents, includeSalary);
        return new GeneratedDocument(baseName + ".pdf", "application/pdf", content);
    }

    // Terminated staff are no longer existing employees.
    private List<Employee> selectEmployees(Long branchId, Long agentId) {
        List<Employee> employees;
        if (agentId != null && branchId != null) {
            employees = employeeRepository.findByBranchIdAndAgentId(branchId, agentId);
        } else if (agentId != null) {
            employees = employeeRepository.findByAgentId(agentId);
        } else if (branchId != null) {
            employees = employeeRepository.findByBranchId(branchId);
        } else {
            employees = employeeRepository.findAllExisting();
        }

        return employees.stream()
                .filter(employee -> !"TERMINATED".equals(employee.getEmploymentStatus()))
                .sorted(Comparator.comparing(Employee::getFullName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private byte[] toPdf(
            List<Employee> employees,
            Map<Long, String> branches,
            Map<Long, String> departments,
            Map<Long, String> agents,
            boolean includeSalary
    ) {
        Map<String, List<Employee>> byBranch = new TreeMap<>(employees.stream()
                .collect(Collectors.groupingBy(employee -> branches.getOrDefault(employee.getBranchId(), "-"))));

        float[] columns = includeSalary ? PDF_COLUMNS : PDF_COLUMNS_NO_SALARY;
        List<String> headings = new ArrayList<>(List.of("Code", "Name", "Phone", "Department", "Agent"));
        if (includeSalary) {
            headings.add("Salary");
        }

        try (PdfDocumentWriter writer = new PdfDocumentWriter()) {
            companyService.writeLetterhead(writer);
            writer.title("Employee List");
            writer.subtitle("Date: " + businessClock.today() + "   Employees: " + employees.size());
            writer.divider();

            if (byBranch.isEmpty()) {
                writer.subtitle("No employees to list.");
            }

            for (Map.Entry<String, List<Employee>> branch : byBranch.entrySet()) {
                writer.sectionHeading(branch.getKey() + " (" + branch.getValue().size() + ")");
                writer.tableRow(columns, headings, true);

                for (Employee employee : branch.getValue()) {
                    List<String> cells = new ArrayList<>(List.of(
                            employee.getEmployeeNumber(),
                            employee.getFullName(),
                            orDash(employee.getPhone()),
                            departments.getOrDefault(employee.getDepartmentId(), "-"),
                            agents.getOrDefault(employee.getAgentId(), "-")));
                    if (includeSalary) {
                        cells.add(employee.getBasicSalary() == null ? "-" : format(employee.getBasicSalary()));
                    }
                    writer.tableRow(columns, cells, false);
                }
            }

            return writer.build();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to generate employee list PDF", e);
        }
    }

    private byte[] toCsv(
            List<Employee> employees,
            Map<Long, String> branches,
            Map<Long, String> departments,
            Map<Long, String> agents,
            boolean includeSalary
    ) {
        StringBuilder csv = new StringBuilder();
        csv.append("Employee Code,Name,Phone No.,Branch,Department,Agent Name");
        csv.append(includeSalary ? ",Basic Salary\r\n" : "\r\n");

        for (Employee employee : employees) {
            csv.append(cell(employee.getEmployeeNumber())).append(',')
                    .append(cell(employee.getFullName())).append(',')
                    .append(phoneCell(employee.getPhone())).append(',')
                    .append(cell(branches.get(employee.getBranchId()))).append(',')
                    .append(cell(departments.get(employee.getDepartmentId()))).append(',')
                    .append(cell(agents.get(employee.getAgentId())));
            if (includeSalary) {
                csv.append(',').append(employee.getBasicSalary() == null ? "" : employee.getBasicSalary().toPlainString());
            }
            csv.append("\r\n");
        }

        // The byte order mark is how Excel knows the file is UTF-8.
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes(new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
        out.writeBytes(csv.toString().getBytes(StandardCharsets.UTF_8));
        return out.toByteArray();
    }

    // Names and the like are typed in by other users, and Excel runs a cell that starts with = + - @
    // as a formula, so such a cell is marked as plain text before it is quoted.
    private static String cell(String value) {
        if (value == null) {
            return "";
        }
        String safe = !value.isEmpty() && "=+-@\t\r".indexOf(value.charAt(0)) >= 0 ? "'" + value : value;
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }

    // A real phone number stays as typed: "+254..." is not a formula, and a leading ' would spoil it.
    private static String phoneCell(String phone) {
        if (phone != null && PHONE_NUMBER.matcher(phone).matches()) {
            return "\"" + phone + "\"";
        }
        return cell(phone);
    }

    private static String orDash(String value) {
        return value != null && !value.isBlank() ? value : "-";
    }
}
