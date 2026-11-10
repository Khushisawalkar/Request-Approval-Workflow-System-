package com.workflow.repository;

import com.workflow.model.RequestAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RequestAuditLogRepository extends JpaRepository<RequestAuditLog, Long> {

    List<RequestAuditLog> findByRequestIdOrderByTimestampDesc(Long requestId);
}

