package com.workflow.dto;

import com.workflow.model.RequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestAuditLogDto {
    private Long id;
    private Long requestId;
    private String action;
    private String actorUsername;
    private String actorRole;
    private RequestStatus fromStatus;
    private RequestStatus toStatus;
    private String comment;
    private LocalDateTime timestamp;
}
