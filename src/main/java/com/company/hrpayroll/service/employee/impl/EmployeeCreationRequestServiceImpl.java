// service/employee/impl/EmployeeCreationRequestServiceImpl.java
package com.company.hrpayroll.service.employee.impl;

import com.company.hrpayroll.dto.request.employee.EmployeeRequest;
import com.company.hrpayroll.dto.response.employee.EmployeeCreationRequestResponse;
import com.company.hrpayroll.dto.response.employee.EmployeeResponse;
import com.company.hrpayroll.entity.auth.Role;
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
import com.company.hrpayroll.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Employees can be proposed by a Branch Manager, an Agent (or the legacy HR role) and only exist
 * once an Admin approves — approval is what actually creates the Employee row.
 *
 *  - A Branch Manager's request is always for their OWN branch.
 *  - An Agent's request is always under THEIR OWN agent record.
 *  - When someone other than the agent submits staff for an agent, that agent is notified.
 */
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
    private final NotificationService notificationService;

    // Notification types for this workflow. The bell links on these, so the
    // frontend (notification-bell.tsx) keys on the same strings.
    private static final String TYPE_SUBMITTED = "EMPLOYEE_REQUEST_SUBMITTED";         // -> admins
    private static final String TYPE_APPROVED = "EMPLOYEE_REQUEST_APPROVED";           // -> requester
    private static final String TYPE_REJECTED = "EMPLOYEE_REQUEST_REJECTED";           // -> requester
    private static final String TYPE_AGENT_NOTICE = "EMPLOYEE_REQUEST_AGENT_NOTICE";   // -> the agent
    private static final String TYPE_AGENT_APPROVED = "EMPLOYEE_REQUEST_AGENT_APPROVED"; // -> the agent

    @Override
    public EmployeeCreationRequestResponse submit(EmployeeRequest original, Long requestedByUserId) {
        User submitter = userRepository.findById(requestedByUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + requestedByUserId));
        EmployeeRequest request = scopeToSubmitter(original, submitter);

        // Early, friendly validation. The full set of checks runs again on approval
        // (EmployeeService.createEmployee), because data can change while pending.
        Branch branch = branchRepository.findById(request.branchId())
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found with id: " + request.branchId()));
        if (!"ACTIVE".equals(branch.getStatus())) {
            throw new InvalidOperationException("Branch '" + branch.getName() + "' is inactive");
        }

        // Departments are company-wide, so only their existence matters.
        if (request.departmentId() != null && !departmentRepository.existsById(request.departmentId())) {
            throw new ResourceNotFoundException("Department not found with id: " + request.departmentId());
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

        // Tell every active admin there is something to review.
        String message = userName(requestedByUserId) + " requested to add " + request.fullName()
                + " (" + branch.getName() + "). Review and approve.";
        adminUsers().forEach(admin ->
                notificationService.notifyInApp(admin.getId(), TYPE_SUBMITTED, message, saved.getId()));

        // Tell the agent when somebody else is adding staff under their agency.
        if (request.agentId() != null && !request.agentId().equals(submitter.getAgentId())) {
            userRepository.findByAgentId(request.agentId())
                    .filter(User::isActive)
                    .ifPresent(agentUser -> notificationService.notifyInApp(
                            agentUser.getId(), TYPE_AGENT_NOTICE,
                            userName(requestedByUserId) + " submitted " + request.fullName()
                                    + " to join your agency at " + branch.getName()
                                    + ". Awaiting admin approval.",
                            saved.getId()));
        }

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
        EmployeeCreationRequest saved = requestRepository.save(entity);

        // The admins' "needs review" notifications are done — clear them — and the
        // requester (and the agent, if different) is told the outcome.
        notificationService.resolveByReference(TYPE_SUBMITTED, id);
        notificationService.notifyInApp(saved.getRequestedByUserId(), TYPE_APPROVED,
                "Approved by " + userName(reviewerUserId) + ": " + employee.fullName()
                        + " was added as " + employee.employeeNumber() + ".",
                id);
        notifyAgent(entity, TYPE_AGENT_APPROVED,
                employee.fullName() + " was approved and added under your agency as "
                        + employee.employeeNumber() + ".");

        return toResponse(saved);
    }

    @Override
    @Transactional
    public EmployeeCreationRequestResponse reject(Long id, Long reviewerUserId, String reason) {
        EmployeeCreationRequest entity = findPendingOrThrow(id);

        entity.setStatus(EmployeeRequestStatus.REJECTED);
        entity.setReviewedByUserId(reviewerUserId);
        entity.setReviewedAt(LocalDateTime.now());
        entity.setRejectionReason(reason);
        EmployeeCreationRequest saved = requestRepository.save(entity);

        notificationService.resolveByReference(TYPE_SUBMITTED, id);
        notificationService.notifyInApp(saved.getRequestedByUserId(), TYPE_REJECTED,
                "Rejected by " + userName(reviewerUserId) + ": " + entity.getFullName()
                        + " — " + reason,
                id);

        return toResponse(saved);
    }

    // ---- helpers ----

    // A Branch Manager can only propose staff for their own branch, and an Agent only under their
    // own agent record — whatever the form says. Every other submitter is taken at their word.
    private EmployeeRequest scopeToSubmitter(EmployeeRequest r, User submitter) {
        Long branchId = r.branchId();
        Long agentId = r.agentId();
        BigDecimal basicSalary = r.basicSalary();

        if (submitter.getRole() == Role.BRANCH_MANAGER) {
            if (submitter.getBranchId() == null) {
                throw new InvalidOperationException(
                        "Your account is not assigned to a branch. Ask an Admin to assign you one.");
            }
            // The agent is who the payment formula pays through, and the formula decides the pay.
            if (agentId == null) {
                throw new InvalidOperationException("Choose the agent this employee belongs to");
            }
            branchId = submitter.getBranchId();
            basicSalary = null;
        } else if (submitter.getRole() == Role.AGENT) {
            if (submitter.getAgentId() == null) {
                throw new InvalidOperationException("Your account is not linked to an agent record.");
            }
            agentId = submitter.getAgentId();
        }

        if (Objects.equals(branchId, r.branchId()) && Objects.equals(agentId, r.agentId())
                && Objects.equals(basicSalary, r.basicSalary())) {
            return r;
        }

        return new EmployeeRequest(
                r.fullName(), r.nationalId(), r.dateOfBirth(), r.gender(), r.maritalStatus(),
                r.phone(), r.email(), r.address(), r.kraPin(), r.shifNumber(),
                r.nssfNumber(), r.nitaNumber(), branchId, r.departmentId(), agentId,
                r.position(), r.employmentType(), basicSalary, r.dateHired(),
                r.bankName(), r.bankAccountNumber(), r.emergencyContactName(),
                r.emergencyContactPhone(), null);
    }

    // Tells the agent's login about an outcome, unless the agent is the one who asked.
    private void notifyAgent(EmployeeCreationRequest entity, String type, String message) {
        if (entity.getAgentId() == null) {
            return;
        }
        userRepository.findByAgentId(entity.getAgentId())
                .filter(User::isActive)
                .filter(agentUser -> !agentUser.getId().equals(entity.getRequestedByUserId()))
                .ifPresent(agentUser ->
                        notificationService.notifyInApp(agentUser.getId(), type, message, entity.getId()));
    }

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
                e.getEmergencyContactPhone(), null);
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
                e.getBranchId(),
                branchName,
                departmentName,
                agentName,
                toEmployeeRequest(e));
    }

    // Everyone who can approve: active ADMIN and SUPER_ADMIN accounts.
    private List<User> adminUsers() {
        return java.util.stream.Stream.concat(
                        userRepository.findByRole(Role.ADMIN).stream(),
                        userRepository.findByRole(Role.SUPER_ADMIN).stream())
                .filter(User::isActive)
                .toList();
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