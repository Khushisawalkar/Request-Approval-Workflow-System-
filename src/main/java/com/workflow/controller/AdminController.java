package com.workflow.controller;

import com.workflow.dto.ApiResponse;
import com.workflow.dto.UserSummaryDto;
import com.workflow.model.RequestStatus;
import com.workflow.repository.RequestRepository;
import com.workflow.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Admin Operations", description = "System metrics, role management, and organizational workflow visibility")
public class AdminController {

    private final RequestRepository requestRepository;
    private final UserService userService;

    @GetMapping("/stats")
    @Operation(summary = "Get system-wide request workflow metrics and breakdown")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getWorkflowStats() {
        Map<String, Object> stats = new HashMap<>();
        long total = requestRepository.count();
        long pending = requestRepository.countByStatus(RequestStatus.PENDING);
        long approved = requestRepository.countByStatus(RequestStatus.APPROVED);
        long rejected = requestRepository.countByStatus(RequestStatus.REJECTED);
        long cancelled = requestRepository.countByStatus(RequestStatus.CANCELLED);

        stats.put("totalRequests", total);
        stats.put("pendingRequests", pending);
        stats.put("approvedRequests", approved);
        stats.put("rejectedRequests", rejected);
        stats.put("cancelledRequests", cancelled);

        return ResponseEntity.ok(ApiResponse.success("Workflow statistics retrieved", stats));
    }

    @GetMapping("/users")
    @Operation(summary = "List all registered users across the organization")
    public ResponseEntity<ApiResponse<List<UserSummaryDto>>> getAllUsers() {
        return ResponseEntity.ok(ApiResponse.success("Users retrieved", userService.getAllUsers()));
    }

    @GetMapping("/managers")
    @Operation(summary = "List all eligible managers available for assignment")
    public ResponseEntity<ApiResponse<List<UserSummaryDto>>> getManagers() {
        return ResponseEntity.ok(ApiResponse.success("Managers retrieved", userService.getManagers()));
    }
}
