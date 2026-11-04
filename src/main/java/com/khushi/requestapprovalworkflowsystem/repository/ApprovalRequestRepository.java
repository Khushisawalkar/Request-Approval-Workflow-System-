package com.khushi.requestapprovalworkflowsystem.repository;

import com.khushi.requestapprovalworkflowsystem.entity.ApprovalRequest;
import com.khushi.requestapprovalworkflowsystem.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, Long> {
    List<ApprovalRequest> findByEmployee(User employee);
    List<ApprovalRequest> findByStatus(com.khushi.requestapprovalworkflowsystem.enums.RequestStatus status);
}
