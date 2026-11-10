package com.workflow.service;

import com.workflow.dto.*;
import com.workflow.exception.InvalidStatusTransitionException;
import com.workflow.exception.ResourceNotFoundException;
import com.workflow.exception.UnauthorizedActionException;
import com.workflow.model.*;
import com.workflow.repository.RequestRepository;
import com.workflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestService {

    private final RequestRepository requestRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final UserService userService;

    @Transactional
    public RequestResponseDto createRequest(CreateRequestDto dto, String username) {
        User requester;
        if (username != null && !username.isBlank()) {
            requester = userRepository.findByUsername(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        } else if (dto.getEmployeeId() != null) {
            requester = userRepository.findById(dto.getEmployeeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + dto.getEmployeeId()));
        } else {
            throw new IllegalArgumentException("Requester identity is missing");
        }

        User assignedManager = null;
        if (dto.getAssignedManagerId() != null) {
            assignedManager = userRepository.findById(dto.getAssignedManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Manager not found with id: " + dto.getAssignedManagerId()));
        } else if (requester.getManager() != null) {
            assignedManager = requester.getManager();
        } else {
            List<User> managers = userRepository.findByRole(RoleName.ROLE_MANAGER);
            if (!managers.isEmpty()) {
                assignedManager = managers.get(0);
            }
        }

        Request request = Request.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .category(dto.getCategory() != null ? dto.getCategory() : RequestCategory.GENERAL)
                .priority(dto.getPriority() != null ? dto.getPriority() : RequestPriority.MEDIUM)
                .status(RequestStatus.PENDING)
                .submittedBy(requester)
                .assignedManager(assignedManager)
                .build();

        Request saved = requestRepository.save(request);

        auditLogService.record(
                saved.getId(),
                "SUBMITTED",
                requester.getUsername(),
                getPrimaryRole(requester),
                null,
                RequestStatus.PENDING,
                "Request created and submitted for approval"
        );

        log.info("Request [{}] created by user [{}] with priority [{}]", saved.getId(), requester.getUsername(), saved.getPriority());
        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public PagedResponse<RequestResponseDto> getRequests(
            RequestStatus status,
            RequestCategory category,
            RequestPriority priority,
            String username,
            Pageable pageable
    ) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        boolean isAdmin = user.getRoles().contains(RoleName.ROLE_ADMIN);
        boolean isManager = user.getRoles().contains(RoleName.ROLE_MANAGER);

        Page<Request> page;
        if (isAdmin) {
            page = requestRepository.findAllWithFilters(status, category, priority, null, null, pageable);
        } else if (isManager) {
            page = requestRepository.findForManager(status, category, priority, user.getId(), pageable);
        } else {
            page = requestRepository.findAllWithFilters(status, category, priority, null, user.getId(), pageable);
        }

        List<RequestResponseDto> content = page.getContent().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return PagedResponse.<RequestResponseDto>builder()
                .content(content)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public RequestResponseDto getRequestById(Long id, String username) {
        Request request = getRequestEntity(id);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        validateViewPermission(request, user);
        return mapToDto(request);
    }

    @Transactional
    public RequestResponseDto updateRequest(Long id, UpdateRequestDto dto, String username) {
        Request request = getRequestEntity(id);

        if (!request.getSubmittedBy().getUsername().equals(username)) {
            throw new UnauthorizedActionException("Only the original submitter can update this request");
        }

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new InvalidStatusTransitionException("Cannot update a request that is already " + request.getStatus());
        }

        if (dto.getTitle() != null && !dto.getTitle().isBlank()) {
            request.setTitle(dto.getTitle());
        }
        if (dto.getDescription() != null && !dto.getDescription().isBlank()) {
            request.setDescription(dto.getDescription());
        }
        if (dto.getCategory() != null) {
            request.setCategory(dto.getCategory());
        }
        if (dto.getPriority() != null) {
            request.setPriority(dto.getPriority());
        }

        Request updated = requestRepository.save(request);

        auditLogService.record(
                id,
                "UPDATED",
                username,
                "ROLE_EMPLOYEE",
                RequestStatus.PENDING,
                RequestStatus.PENDING,
                "Request details updated"
        );

        log.info("Request [{}] updated by submitter [{}]", id, username);
        return mapToDto(updated);
    }

    @Transactional
    public RequestResponseDto approveRequest(Long id, WorkflowDecisionDto decision, String approverUsername) {
        Request request = getRequestEntity(id);
        User approver = userRepository.findByUsername(approverUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Approver not found: " + approverUsername));

        validateDecisionPermission(request, approver);

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new InvalidStatusTransitionException("Cannot approve request in status: " + request.getStatus());
        }

        String comment = (decision != null && decision.getComment() != null && !decision.getComment().isBlank())
                ? decision.getComment().trim()
                : "Approved";

        request.setStatus(RequestStatus.APPROVED);
        request.setDecisionComment(comment);
        request.setAssignedManager(approver);
        Request saved = requestRepository.save(request);

        auditLogService.record(
                id,
                "APPROVED",
                approverUsername,
                getPrimaryRole(approver),
                RequestStatus.PENDING,
                RequestStatus.APPROVED,
                comment
        );

        log.info("Request [{}] APPROVED by [{}]", id, approverUsername);
        return mapToDto(saved);
    }

    @Transactional
    public RequestResponseDto rejectRequest(Long id, WorkflowDecisionDto decision, String rejectorUsername) {
        if (decision == null || decision.getComment() == null || decision.getComment().trim().isEmpty()) {
            throw new IllegalArgumentException("Rejection reason comment is mandatory");
        }

        Request request = getRequestEntity(id);
        User rejector = userRepository.findByUsername(rejectorUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + rejectorUsername));

        validateDecisionPermission(request, rejector);

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new InvalidStatusTransitionException("Cannot reject request in status: " + request.getStatus());
        }

        String comment = decision.getComment().trim();
        request.setStatus(RequestStatus.REJECTED);
        request.setDecisionComment(comment);
        request.setAssignedManager(rejector);
        Request saved = requestRepository.save(request);

        auditLogService.record(
                id,
                "REJECTED",
                rejectorUsername,
                getPrimaryRole(rejector),
                RequestStatus.PENDING,
                RequestStatus.REJECTED,
                comment
        );

        log.info("Request [{}] REJECTED by [{}]", id, rejectorUsername);
        return mapToDto(saved);
    }

    @Transactional
    public RequestResponseDto cancelRequest(Long id, String username) {
        Request request = getRequestEntity(id);

        if (!request.getSubmittedBy().getUsername().equals(username)) {
            throw new UnauthorizedActionException("Only the requester can cancel their request");
        }

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new InvalidStatusTransitionException("Cannot cancel request that has already been " + request.getStatus());
        }

        request.setStatus(RequestStatus.CANCELLED);
        request.setDecisionComment("Cancelled by submitter");
        Request saved = requestRepository.save(request);

        auditLogService.record(
                id,
                "CANCELLED",
                username,
                "ROLE_EMPLOYEE",
                RequestStatus.PENDING,
                RequestStatus.CANCELLED,
                "Cancelled by requester"
        );

        log.info("Request [{}] CANCELLED by requester [{}]", id, username);
        return mapToDto(saved);
    }

    // Backwards-compatible legacy method for static UI and existing tests
    @Transactional
    public Request processRequest(Long requestId, ActionRequestDto dto) {
        User manager = userRepository.findById(dto.getManagerId())
                .orElseThrow(() -> new ResourceNotFoundException("Manager not found with id: " + dto.getManagerId()));

        Request request = getRequestEntity(requestId);
        if (request.getStatus() != RequestStatus.PENDING) {
            throw new InvalidStatusTransitionException("Request is already processed: " + request.getStatus());
        }

        if ("APPROVE".equalsIgnoreCase(dto.getAction())) {
            approveRequest(requestId, new WorkflowDecisionDto(dto.getComment()), manager.getUsername());
        } else if ("REJECT".equalsIgnoreCase(dto.getAction())) {
            String comment = (dto.getComment() != null && !dto.getComment().isBlank()) ? dto.getComment() : "Rejected by manager";
            rejectRequest(requestId, new WorkflowDecisionDto(comment), manager.getUsername());
        } else {
            throw new IllegalArgumentException("Invalid action: " + dto.getAction());
        }

        return getRequestEntity(requestId);
    }

    @Transactional(readOnly = true)
    public List<RequestResponseDto> getAllRequestsList() {
        return requestRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RequestResponseDto> getRequestsByEmployeeId(Long employeeId) {
        return requestRepository.findBySubmittedById(employeeId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private void validateViewPermission(Request request, User user) {
        boolean isAdmin = user.getRoles().contains(RoleName.ROLE_ADMIN);
        boolean isRequester = request.getSubmittedBy().getId().equals(user.getId());
        boolean isAssignedManager = request.getAssignedManager() != null &&
                request.getAssignedManager().getId().equals(user.getId());

        if (!isAdmin && !isRequester && !isAssignedManager) {
            throw new UnauthorizedActionException("You are not authorized to view this request");
        }
    }

    private void validateDecisionPermission(Request request, User user) {
        boolean isAdmin = user.getRoles().contains(RoleName.ROLE_ADMIN);
        boolean isManager = user.getRoles().contains(RoleName.ROLE_MANAGER);

        if (!isAdmin && !isManager) {
            throw new UnauthorizedActionException("Only managers or admins can make approval decisions");
        }

        // Prevent approving own request (self-approval prevention)
        if (!isAdmin && request.getSubmittedBy().getId().equals(user.getId())) {
            throw new UnauthorizedActionException("Self-approval is strictly prohibited");
        }

        // If explicitly assigned to a specific manager, only that manager or admin can decide
        if (!isAdmin && request.getAssignedManager() != null && !request.getAssignedManager().getId().equals(user.getId())) {
            throw new UnauthorizedActionException("This request is assigned to a different manager");
        }
    }

    private String getPrimaryRole(User user) {
        if (user.getRoles().contains(RoleName.ROLE_ADMIN)) return RoleName.ROLE_ADMIN.name();
        if (user.getRoles().contains(RoleName.ROLE_MANAGER)) return RoleName.ROLE_MANAGER.name();
        return RoleName.ROLE_EMPLOYEE.name();
    }

    private Request getRequestEntity(Long id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found with id: " + id));
    }

    public RequestResponseDto mapToDto(Request request) {
        return RequestResponseDto.builder()
                .id(request.getId())
                .title(request.getTitle())
                .description(request.getDescription())
                .category(request.getCategory())
                .priority(request.getPriority())
                .status(request.getStatus())
                .submittedBy(userService.mapToSummary(request.getSubmittedBy()))
                .assignedManager(userService.mapToSummary(request.getAssignedManager()))
                .decisionComment(request.getDecisionComment())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .build();
    }
}
