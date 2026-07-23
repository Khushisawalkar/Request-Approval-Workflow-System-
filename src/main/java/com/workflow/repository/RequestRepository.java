package com.workflow.repository;

import com.workflow.model.Request;
import com.workflow.model.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RequestRepository extends JpaRepository<Request, Long> {
    List<Request> findByStatus(RequestStatus status);
}
