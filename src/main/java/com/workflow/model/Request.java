package com.workflow.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "requests")
public class Request {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String description;

    @Enumerated(EnumType.STRING)
    private RequestStatus status = RequestStatus.PENDING;

    @ManyToOne
    @JoinColumn(name = "requestor_id")
    private User requestor;

    @ManyToOne
    @JoinColumn(name = "approver_id")
    private User approver;
}
