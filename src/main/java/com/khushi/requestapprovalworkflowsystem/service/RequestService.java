package com.khushi.requestapprovalworkflowsystem.service;

import com.khushi.requestapprovalworkflowsystem.dto.ActionRequestDto;
import com.khushi.requestapprovalworkflowsystem.dto.CreateRequestDto;
import com.khushi.requestapprovalworkflowsystem.entity.ApprovalRequest;
import com.khushi.requestapprovalworkflowsystem.entity.User;
import com.khushi.requestapprovalworkflowsystem.enums.RequestStatus;
import com.khushi.requestapprovalworkflowsystem.enums.Role;
import com.khushi.requestapprovalworkflowsystem.repository.ApprovalRequestRepository;
import com.khushi.requestapprovalworkflowsystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RequestService {

    private final ApprovalRequestRepository requestRepository;
    private final UserRepository userRepository;

    public ApprovalRequest createRequest(CreateRequestDto dto) {
        User employee = userRepository.findById(dto.getEmployeeId())
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        if (employee.getRole() != Role.EMPLOYEE) {
            throw new RuntimeException("Only employees can create requests");
        }

        ApprovalRequest request = ApprovalRequest.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .status(RequestStatus.PENDING)
                .employee(employee)
                .build();

        return requestRepository.save(request);
    }

    public ApprovalRequest processRequest(Long requestId, ActionRequestDto dto) {
        User manager = userRepository.findById(dto.getManagerId())
                .orElseThrow(() -> new RuntimeException("Manager not found"));

        if (manager.getRole() != Role.MANAGER) {
            throw new RuntimeException("Only managers can approve/reject requests");
        }

        ApprovalRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new RuntimeException("Request is already processed");
        }

        if ("APPROVE".equalsIgnoreCase(dto.getAction())) {
            request.setStatus(RequestStatus.APPROVED);
        } else if ("REJECT".equalsIgnoreCase(dto.getAction())) {
            request.setStatus(RequestStatus.REJECTED);
        } else {
            throw new RuntimeException("Invalid action");
        }

        request.setManager(manager);
        return requestRepository.save(request);
    }

    public List<ApprovalRequest> getAllRequests() {
        return requestRepository.findAll();
    }

    public List<ApprovalRequest> getRequestsByEmployee(Long employeeId) {
        User employee = userRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
        return requestRepository.findByEmployee(employee);
    }
}
