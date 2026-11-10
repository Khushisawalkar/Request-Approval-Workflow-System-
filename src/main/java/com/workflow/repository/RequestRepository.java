package com.workflow.repository;

import com.workflow.model.Request;
import com.workflow.model.RequestCategory;
import com.workflow.model.RequestPriority;
import com.workflow.model.RequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RequestRepository extends JpaRepository<Request, Long> {

    @Query("SELECT r FROM Request r WHERE " +
           "(:status IS NULL OR r.status = :status) AND " +
           "(:category IS NULL OR r.category = :category) AND " +
           "(:priority IS NULL OR r.priority = :priority) AND " +
           "(:assignedManagerId IS NULL OR r.assignedManager.id = :assignedManagerId) AND " +
           "(:submittedById IS NULL OR r.submittedBy.id = :submittedById)")
    Page<Request> findAllWithFilters(
            @Param("status") RequestStatus status,
            @Param("category") RequestCategory category,
            @Param("priority") RequestPriority priority,
            @Param("assignedManagerId") Long assignedManagerId,
            @Param("submittedById") Long submittedById,
            Pageable pageable
    );

    @Query("SELECT r FROM Request r WHERE " +
           "(:status IS NULL OR r.status = :status) AND " +
           "(:category IS NULL OR r.category = :category) AND " +
           "(:priority IS NULL OR r.priority = :priority) AND " +
           "(r.assignedManager.id = :managerId OR r.submittedBy.id = :managerId)")
    Page<Request> findForManager(
            @Param("status") RequestStatus status,
            @Param("category") RequestCategory category,
            @Param("priority") RequestPriority priority,
            @Param("managerId") Long managerId,
            Pageable pageable
    );

    List<Request> findBySubmittedById(Long submittedById);

    long countByStatus(RequestStatus status);
}
