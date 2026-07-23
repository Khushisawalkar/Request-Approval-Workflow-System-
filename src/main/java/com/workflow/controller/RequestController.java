package com.workflow.controller;

import com.workflow.model.Request;
import com.workflow.service.RequestService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/requests")
public class RequestController {
    private final RequestService requestService;

    public RequestController(RequestService requestService) {
        this.requestService = requestService;
    }

    @PostMapping
    public ResponseEntity<Request> createRequest(@RequestBody Request request) {
        return ResponseEntity.ok(requestService.createRequest(request));
    }

    @GetMapping
    public ResponseEntity<List<Request>> getAllRequests() {
        return ResponseEntity.ok(requestService.getAllRequests());
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<Request> approveRequest(@PathVariable Long id) {
        return ResponseEntity.ok(requestService.approveRequest(id));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<Request> rejectRequest(@PathVariable Long id) {
        return ResponseEntity.ok(requestService.rejectRequest(id));
    }
}
