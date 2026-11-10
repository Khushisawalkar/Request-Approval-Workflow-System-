package com.workflow.controller;

import com.workflow.dto.*;
import com.workflow.model.Request;
import com.workflow.model.RequestCategory;
import com.workflow.model.RequestPriority;
import com.workflow.model.RequestStatus;
import com.workflow.service.AuditLogService;
import com.workflow.service.RequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Request Management & Approval Workflow", description = "Endpoints for request lifecycle and approvals")
public class RequestController {

    private final RequestService requestService;
    private final AuditLogService auditLogService;

    // =========================================================================
    // Modern Enterprise APIs (JWT & RBAC Secured)
    // =========================================================================

    @PostMapping("/api/v1/requests")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Submit a new request")
    public ResponseEntity<ApiResponse<RequestResponseDto>> createRequest(
            @Valid @RequestBody CreateRequestDto dto,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        String username = userDetails != null ? userDetails.getUsername() : null;
        RequestResponseDto response = requestService.createRequest(dto, username);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Request created successfully", response));
    }

    @GetMapping("/api/v1/requests")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Filter and paginate requests based on user role")
    public ResponseEntity<ApiResponse<PagedResponse<RequestResponseDto>>> getRequests(
            @RequestParam(required = false) RequestStatus status,
            @RequestParam(required = false) RequestCategory category,
            @RequestParam(required = false) RequestPriority priority,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        PagedResponse<RequestResponseDto> response = requestService.getRequests(
                status, category, priority, userDetails.getUsername(), pageable
        );
        return ResponseEntity.ok(ApiResponse.success("Requests retrieved", response));
    }

    @GetMapping("/api/v1/requests/{id}")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Get single request details by ID")
    public ResponseEntity<ApiResponse<RequestResponseDto>> getRequestById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        RequestResponseDto response = requestService.getRequestById(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Request details retrieved", response));
    }

    @PutMapping("/api/v1/requests/{id}")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Update pending request (requester only)")
    public ResponseEntity<ApiResponse<RequestResponseDto>> updateRequest(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRequestDto dto,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        RequestResponseDto response = requestService.updateRequest(id, dto, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Request updated successfully", response));
    }

    @PutMapping("/api/v1/requests/{id}/approve")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Approve request with remarks (Manager or Admin only)")
    public ResponseEntity<ApiResponse<RequestResponseDto>> approveRequest(
            @PathVariable Long id,
            @RequestBody(required = false) WorkflowDecisionDto decision,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        RequestResponseDto response = requestService.approveRequest(id, decision, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Request approved successfully", response));
    }

    @PutMapping("/api/v1/requests/{id}/reject")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Reject request with mandatory comment (Manager or Admin only)")
    public ResponseEntity<ApiResponse<RequestResponseDto>> rejectRequest(
            @PathVariable Long id,
            @Valid @RequestBody WorkflowDecisionDto decision,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        RequestResponseDto response = requestService.rejectRequest(id, decision, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Request rejected successfully", response));
    }

    @PutMapping("/api/v1/requests/{id}/cancel")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Cancel request (requester only)")
    public ResponseEntity<ApiResponse<RequestResponseDto>> cancelRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        RequestResponseDto response = requestService.cancelRequest(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Request cancelled successfully", response));
    }

    @GetMapping("/api/v1/requests/{id}/history")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Get audit trail and state history for a request")
    public ResponseEntity<ApiResponse<List<RequestAuditLogDto>>> getRequestHistory(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        requestService.getRequestById(id, userDetails.getUsername()); // Validates view authorization
        List<RequestAuditLogDto> logs = auditLogService.getAuditLogsForRequest(id);
        return ResponseEntity.ok(ApiResponse.success("Audit history retrieved", logs));
    }

    // =========================================================================
    // Backwards-Compatibility Endpoints (Supports Static Dashboard & Legacy APIs)
    // =========================================================================

    @PostMapping("/api/requests")
    public ResponseEntity<?> legacyCreateRequest(
            @RequestBody CreateRequestDto dto,
            Principal principal
    ) {
        String username = principal != null ? principal.getName() : null;
        RequestResponseDto response = requestService.createRequest(dto, username);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/requests")
    public ResponseEntity<List<RequestResponseDto>> legacyGetAllRequests() {
        return ResponseEntity.ok(requestService.getAllRequestsList());
    }

    @PostMapping("/api/requests/{id}/action")
    public ResponseEntity<?> legacyProcessAction(
            @PathVariable Long id,
            @RequestBody ActionRequestDto dto
    ) {
        Request request = requestService.processRequest(id, dto);
        return ResponseEntity.ok(requestService.mapToDto(request));
    }

    @PutMapping("/api/requests/{id}/approve")
    public ResponseEntity<?> legacyApprove(
            @PathVariable Long id,
            @RequestBody(required = false) WorkflowDecisionDto decision,
            Principal principal
    ) {
        String username = principal != null ? principal.getName() : "manager1";
        return ResponseEntity.ok(requestService.approveRequest(id, decision, username));
    }

    @PutMapping("/api/requests/{id}/reject")
    public ResponseEntity<?> legacyReject(
            @PathVariable Long id,
            @RequestBody(required = false) WorkflowDecisionDto decision,
            Principal principal
    ) {
        String username = principal != null ? principal.getName() : "manager1";
        WorkflowDecisionDto dec = decision != null ? decision : new WorkflowDecisionDto("Rejected");
        return ResponseEntity.ok(requestService.rejectRequest(id, dec, username));
    }

    @GetMapping("/api/requests/employee/{employeeId}")
    public ResponseEntity<List<RequestResponseDto>> legacyGetByEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(requestService.getRequestsByEmployeeId(employeeId));
    }
}
