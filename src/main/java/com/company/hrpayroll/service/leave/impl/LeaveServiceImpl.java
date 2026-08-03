// service/leave/impl/LeaveServiceImpl.java
package com.company.hrpayroll.service.leave.impl;

import com.company.hrpayroll.dto.request.leave.LeaveApprovalRequest;
import com.company.hrpayroll.dto.request.leave.LeaveRequestCreateDto;
import com.company.hrpayroll.dto.response.leave.LeaveBalanceResponse;
import com.company.hrpayroll.dto.response.leave.LeaveRequestResponse;
import com.company.hrpayroll.dto.response.leave.LeaveTypeResponse;
import com.company.hrpayroll.entity.employee.Employee;
import com.company.hrpayroll.entity.leave.LeaveRequest;
import com.company.hrpayroll.entity.leave.LeaveStatus;
import com.company.hrpayroll.entity.leave.LeaveType;
import com.company.hrpayroll.exception.InvalidOperationException;
import com.company.hrpayroll.exception.ResourceNotFoundException;
import com.company.hrpayroll.mapper.leave.LeaveRequestMapper;
import com.company.hrpayroll.mapper.leave.LeaveTypeMapper;
import com.company.hrpayroll.repository.employee.EmployeeRepository;
import com.company.hrpayroll.repository.leave.LeaveRequestRepository;
import com.company.hrpayroll.repository.leave.LeaveTypeRepository;
import com.company.hrpayroll.service.leave.LeaveService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LeaveServiceImpl implements LeaveService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final EmployeeRepository employeeRepository;
    private final LeaveRequestMapper leaveRequestMapper;
    private final LeaveTypeMapper leaveTypeMapper;

    @Override
    public List<LeaveTypeResponse> getAllLeaveTypes() {
        return leaveTypeRepository.findAll().stream()
                .map(leaveTypeMapper::toLeaveTypeResponse)
                .toList();
    }

    @Override
    public LeaveRequestResponse applyForLeave(Long employeeId, LeaveRequestCreateDto request) {
        if (!employeeRepository.existsById(employeeId)) {
            throw new ResourceNotFoundException("Employee not found with id: " + employeeId);
        }
        if (!leaveTypeRepository.existsById(request.leaveTypeId())) {
            throw new ResourceNotFoundException("Leave type not found with id: " + request.leaveTypeId());
        }
        if (request.endDate().isBefore(request.startDate())) {
            throw new InvalidOperationException("End date cannot be before start date");
        }

        LeaveRequest leaveRequest = leaveRequestMapper.toLeaveRequest(request, employeeId);
        LeaveRequest saved = leaveRequestRepository.save(leaveRequest);
        return toResponseWithNames(saved);
    }

    @Override
    public LeaveRequestResponse approveByManager(Long leaveRequestId, LeaveApprovalRequest request) {
        LeaveRequest leaveRequest = findOrThrow(leaveRequestId);

        if (leaveRequest.getStatus() != LeaveStatus.PENDING_MANAGER) {
            throw new InvalidOperationException(
                    "This request is not awaiting manager approval (current status: " + leaveRequest.getStatus() + ")");
        }

        if (request.approved()) {
            leaveRequest.setStatus(LeaveStatus.PENDING_HR);
        } else {
            leaveRequest.setStatus(LeaveStatus.REJECTED);
            leaveRequest.setRejectionReason(request.rejectionReason());
        }

        LeaveRequest saved = leaveRequestRepository.save(leaveRequest);
        return toResponseWithNames(saved);
    }

    @Override
    public LeaveRequestResponse approveByHr(Long leaveRequestId, LeaveApprovalRequest request) {
        LeaveRequest leaveRequest = findOrThrow(leaveRequestId);

        if (leaveRequest.getStatus() != LeaveStatus.PENDING_HR) {
            throw new InvalidOperationException(
                    "This request is not awaiting HR approval (current status: " + leaveRequest.getStatus() + ")");
        }

        if (request.approved()) {
            leaveRequest.setStatus(LeaveStatus.APPROVED);
        } else {
            leaveRequest.setStatus(LeaveStatus.REJECTED);
            leaveRequest.setRejectionReason(request.rejectionReason());
        }

        LeaveRequest saved = leaveRequestRepository.save(leaveRequest);
        return toResponseWithNames(saved);
    }

    @Override
    public List<LeaveRequestResponse> getLeaveHistoryForEmployee(Long employeeId) {
        return leaveRequestRepository.findByEmployeeId(employeeId).stream()
                .map(this::toResponseWithNames)
                .toList();
    }

    @Override
    public List<LeaveRequestResponse> getPendingManagerApprovals(Long branchId) {
        List<Long> employeeIdsInBranch = employeeRepository.findByBranchId(branchId).stream()
                .map(Employee::getId)
                .toList();

        return leaveRequestRepository.findByStatus(LeaveStatus.PENDING_MANAGER).stream()
                .filter(lr -> employeeIdsInBranch.contains(lr.getEmployeeId()))
                .map(this::toResponseWithNames)
                .toList();
    }

    @Override
    public List<LeaveRequestResponse> getPendingHrApprovals() {
        return leaveRequestRepository.findByStatus(LeaveStatus.PENDING_HR).stream()
                .map(this::toResponseWithNames)
                .toList();
    }

    @Override
    public List<LeaveBalanceResponse> getLeaveBalances(Long employeeId) {
        List<LeaveType> leaveTypes = leaveTypeRepository.findAll();

        return leaveTypes.stream()
                .map(type -> {
                    int usedDays = leaveRequestRepository
                            .findByEmployeeIdAndStatus(employeeId, LeaveStatus.APPROVED).stream()
                            .filter(lr -> lr.getLeaveTypeId().equals(type.getId()))
                            .mapToInt(lr -> (int) ChronoUnit.DAYS.between(lr.getStartDate(), lr.getEndDate()) + 1)
                            .sum();

                    int entitled = type.getDefaultEntitlementDays();
                    return new LeaveBalanceResponse(type.getId(), type.getName(), entitled, usedDays,
                            Math.max(entitled - usedDays, 0));
                })
                .toList();
    }

    private LeaveRequest findOrThrow(Long id) {
        return leaveRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found with id: " + id));
    }

    private LeaveRequestResponse toResponseWithNames(LeaveRequest leaveRequest) {
        String employeeName = employeeRepository.findById(leaveRequest.getEmployeeId())
                .map(Employee::getFullName).orElse(null);
        String leaveTypeName = leaveTypeRepository.findById(leaveRequest.getLeaveTypeId())
                .map(LeaveType::getName).orElse(null);
        return leaveRequestMapper.toLeaveRequestResponse(leaveRequest, employeeName, leaveTypeName);
    }
}