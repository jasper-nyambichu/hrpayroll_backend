// service/employee/impl/EmployeeCreationRequestServiceImpl.java
package com.company.hrpayroll.service.employee.impl;

import com.company.hrpayroll.dto.request.employee.EmployeeRequest;
import com.company.hrpayroll.dto.response.employee.EmployeeCreationRequestResponse;
import com.company.hrpayroll.dto.response.employee.EmployeeResponse;
import com.company.hrpayroll.entity.auth.User;
import com.company.hrpayroll.entity.branch.Branch;
import com.company.hrpayroll.entity.department.Department;
import com.company.hrpayroll.entity.employee.EmployeeCreationRequest;
import com.company.hrpayroll.entity.employee.EmployeeRequestStatus;
import com.company.hrpayroll.exception.DuplicateResourceException;
import com.company.hrpayroll.exception.InvalidOperationException;
import com.company.hrpayroll.exception.ResourceNotFoundException;
import com.company.hrpayroll.repository.agent.AgentRepository;
import com.company.hrpayroll.repository.auth.UserRepository;
import com.company.hrpayroll.repository.branch.BranchRepository;
import com.company.hrpayroll.repository.department.DepartmentRepository;
import com.company.hrpayroll.repository.employee.EmployeeCreationRequestRepository;
import com.company.hrpayroll.repository.employee.EmployeeRepository;
import com.company.hrpayroll.service.employee.EmployeeCreationRequestService;
import com.company.hrpayroll.service.employee.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeCreationRequestServiceImpl implements EmployeeCreationRequestService {

    private final EmployeeCreationRequestRepository requestRepository;
    private final EmployeeRepository employeeRepository;
    private final BranchRepository branchRepository;
    private final DepartmentRepository departmentRepository;
    private final AgentRepository agentRepository;
    private final UserRepository userRepository;
    private final EmployeeService employeeService;

    @Override
    public EmployeeCreationRequestResponse submit(EmployeeRequest request, Long requestedByUserId) {
        // Early, friendly validation. The full set of checks runs again on approval
        // (EmployeeService.createEmployee), because data can change while pending.
        branchRepository.findById(request.branchId())
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found with id: " + request.branchId()));

        if (request.departmentId() != null) {
            Department department = departmentRepository.findById(request.departmentId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Department not found with id: " + request.departmentId()));
            if (!department.getBranchId().equals(request.branchId())) {
                throw new InvalidOperationException(
                        "Department '" + department.getName() + "' does not belong to the selected branch");
            }
        }

        if (request.agentId() != null && !agentRepository.existsById(request.agentId())) {
            throw new ResourceNotFoundException("Agent not found with id: " + request.agentId());
        }

        String nationalId = request.nationalId();
        if (nationalId != null && !nationalId.isBlank()) {
            if (employeeRepository.existsByNationalId(nationalId)) {
                throw new DuplicateResourceException(
                        "An employee with national ID '" + nationalId + "' already exists");
            }
            if (requestRepository.existsByNationalIdAndStatus(nationalId, EmployeeRequestStatus.PENDING)) {
                throw new DuplicateResourceException(
                        "A pending request for national ID '" + nationalId + "' already exists");
            }
        }

        EmployeeCreationRequest saved = requestRepository.save(toEntity(request, requestedByUserId));
        return toResponse(saved);
    }

    @Override
    public List<EmployeeCreationRequestResponse> list(EmployeeRequestStatus statusOrNull) {
        List<EmployeeCreationRequest> rows = statusOrNull == null
                ? requestRepository.findAllByOrderByCreatedAtDesc()
                : requestRepository.findByStatusOrderByCreatedAtDesc(statusOrNull);
        return rows.stream().map(this::toResponse).toList();
    }

    @Override
    public List<EmployeeCreationRequestResponse> listMine(Long userId) {
        return requestRepository.findByRequestedByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public EmployeeCreationRequestResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    // One transaction: if createEmployee fails (e.g. the national ID was taken in the
    // meantime) nothing is saved and the request stays PENDING.
    @Override
    @Transactional
    public EmployeeCreationRequestResponse approve(Long id, Long reviewerUserId) {
        EmployeeCreationRequest entity = findPendingOrThrow(id);

        EmployeeResponse employee = employeeService.createEmployee(toEmployeeRequest(entity));

        entity.setStatus(EmployeeRequestStatus.APPROVED);
        entity.setReviewedByUserId(reviewerUserId);
        entity.setReviewedAt(LocalDateTime.now());
        entity.setCreatedEmployeeId(employee.id());
        return toResponse(requestRepository.save(entity));
    }

    @Override
    @Transactional
    public EmployeeCreationRequestResponse reject(Long id, Long reviewerUserId, String reason) {
        EmployeeCreationRequest entity = findPendingOrThrow(id);

        entity.setStatus(EmployeeRequestStatus.REJECTED);
        entity.setReviewedByUserId(reviewerUserId);
        entity.setReviewedAt(LocalDateTime.now());
        entity.setRejectionReason(reason);
        return toResponse(requestRepository.save(entity));
    }

    // ---- helpers ----

    private EmployeeCreationRequest findOrThrow(Long id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee request not found with id: " + id));
    }

    private EmployeeCreationRequest findPendingOrThrow(Long id) {
        EmployeeCreationRequest entity = findOrThrow(id);
        if (entity.getStatus() != EmployeeRequestStatus.PENDING) {
            throw new InvalidOperationException(
                    "This request has already been reviewed (current status: " + entity.getStatus() + ")");
        }
        return entity;
    }

    private EmployeeCreationRequest toEntity(EmployeeRequest r, Long requestedByUserId) {
        return EmployeeCreationRequest.builder()
                .requestedByUserId(requestedByUserId)
                .fullName(r.fullName())
                .nationalId(r.nationalId())
                .dateOfBirth(r.dateOfBirth())
                .gender(r.gender())
                .maritalStatus(r.maritalStatus())
                .phone(r.phone())
                .email(r.email())
                .address(r.address())
                .kraPin(r.kraPin())
                .shifNumber(r.shifNumber())
                .nssfNumber(r.nssfNumber())
                .nitaNumber(r.nitaNumber())
                .branchId(r.branchId())
                .departmentId(r.departmentId())
                .agentId(r.agentId())
                .position(r.position())
                .employmentType(r.employmentType())
                .basicSalary(r.basicSalary())
                .dateHired(r.dateHired())
                .bankName(r.bankName())
                .bankAccountNumber(r.bankAccountNumber())
                .emergencyContactName(r.emergencyContactName())
                .emergencyContactPhone(r.emergencyContactPhone())
                .build();
    }

    private EmployeeRequest toEmployeeRequest(EmployeeCreationRequest e) {
        return new EmployeeRequest(
                e.getFullName(), e.getNationalId(), e.getDateOfBirth(), e.getGender(), e.getMaritalStatus(),
                e.getPhone(), e.getEmail(), e.getAddress(), e.getKraPin(), e.getShifNumber(),
                e.getNssfNumber(), e.getNitaNumber(), e.getBranchId(), e.getDepartmentId(), e.getAgentId(),
                e.getPosition(), e.getEmploymentType(), e.getBasicSalary(), e.getDateHired(),
                e.getBankName(), e.getBankAccountNumber(), e.getEmergencyContactName(),
                e.getEmergencyContactPhone());
    }

    private EmployeeCreationRequestResponse toResponse(EmployeeCreationRequest e) {
        String branchName = branchRepository.findById(e.getBranchId()).map(Branch::getName).orElse(null);
        String departmentName = e.getDepartmentId() == null ? null
                : departmentRepository.findById(e.getDepartmentId()).map(Department::getName).orElse(null);
        String agentName = e.getAgentId() == null ? null
                : agentRepository.findById(e.getAgentId()).map(a -> a.getName()).orElse(null);

        return new EmployeeCreationRequestResponse(
                e.getId(),
                e.getStatus(),
                e.getRequestedByUserId(),
                userName(e.getRequestedByUserId()),
                e.getCreatedAt(),
                e.getReviewedByUserId(),
                userName(e.getReviewedByUserId()),
                e.getReviewedAt(),
                e.getRejectionReason(),
                e.getCreatedEmployeeId(),
                branchName,
                departmentName,
                agentName,
                toEmployeeRequest(e));
    }

    private String userName(Long userId) {
        if (userId == null) {
            return null;
        }
        return userRepository.findById(userId)
                .map((User u) -> u.getDisplayName() != null ? u.getDisplayName() : u.getUsername())
                .orElse(null);
    }
}