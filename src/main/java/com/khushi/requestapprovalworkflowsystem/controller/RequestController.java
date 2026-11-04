package com.khushi.requestapprovalworkflowsystem.controller;

import com.khushi.requestapprovalworkflowsystem.dto.ActionRequestDto;
import com.khushi.requestapprovalworkflowsystem.dto.CreateRequestDto;
import com.khushi.requestapprovalworkflowsystem.entity.ApprovalRequest;
import com.khushi.requestapprovalworkflowsystem.service.RequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/requests")
@RequiredArgsConstructor
@CrossOrigin("*")
public class RequestController {

    private final RequestService requestService;

    @PostMapping
    public ResponseEntity<ApprovalRequest> createRequest(@RequestBody CreateRequestDto dto) {
        return ResponseEntity.ok(requestService.createRequest(dto));
    }

    @PostMapping("/{id}/action")
    public ResponseEntity<ApprovalRequest> processRequest(@PathVariable Long id, @RequestBody ActionRequestDto dto) {
        return ResponseEntity.ok(requestService.processRequest(id, dto));
    }

    @GetMapping
    public ResponseEntity<List<ApprovalRequest>> getAllRequests() {
        return ResponseEntity.ok(requestService.getAllRequests());
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<ApprovalRequest>> getRequestsByEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(requestService.getRequestsByEmployee(employeeId));
    }
}
