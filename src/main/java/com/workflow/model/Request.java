package com.workflow.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Request {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private RequestCategory category = RequestCategory.GENERAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private RequestPriority priority = RequestPriority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private RequestStatus status = RequestStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submitted_by_id", nullable = false)
    private User submittedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_manager_id")
    private User assignedManager;

    @Column(name = "decision_comment", columnDefinition = "TEXT")
    private String decisionComment;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Backwards-compatibility aliases
    public User getRequestor() {
        return submittedBy;
    }

    public void setRequestor(User requestor) {
        this.submittedBy = requestor;
    }

    public User getApprover() {
        return assignedManager;
    }

    public void setApprover(User approver) {
        this.assignedManager = approver;
    }

    public User getEmployee() {
        return submittedBy;
    }

    public void setEmployee(User employee) {
        this.submittedBy = employee;
    }

    public User getManager() {
        return assignedManager;
    }

    public void setManager(User manager) {
        this.assignedManager = manager;
    }
}
