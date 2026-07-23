package com.workflow.service;

import com.workflow.model.Request;
import com.workflow.model.RequestStatus;
import com.workflow.repository.RequestRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RequestService {
    private final RequestRepository requestRepository;

    public RequestService(RequestRepository requestRepository) {
        this.requestRepository = requestRepository;
    }

    public Request createRequest(Request request) {
        request.setStatus(RequestStatus.PENDING);
        return requestRepository.save(request);
    }

    public List<Request> getAllRequests() {
        return requestRepository.findAll();
    }

    public Request approveRequest(Long id) {
        Request request = requestRepository.findById(id).orElseThrow(() -> new RuntimeException("Request not found"));
        request.setStatus(RequestStatus.APPROVED);
        return requestRepository.save(request);
    }

    public Request rejectRequest(Long id) {
        Request request = requestRepository.findById(id).orElseThrow(() -> new RuntimeException("Request not found"));
        request.setStatus(RequestStatus.REJECTED);
        return requestRepository.save(request);
    }
}
