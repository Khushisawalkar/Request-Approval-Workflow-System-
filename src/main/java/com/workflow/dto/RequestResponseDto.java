package com.workflow.dto;

import com.workflow.model.RequestCategory;
import com.workflow.model.RequestPriority;
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
public class RequestResponseDto {
    private Long id;
    private String title;
    private String description;
    private RequestCategory category;
    private RequestPriority priority;
    private RequestStatus status;
    private UserSummaryDto submittedBy;
    private UserSummaryDto assignedManager;
    private String decisionComment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Backwards-compatible employee field for old UI
    public UserSummaryDto getEmployee() {
        return submittedBy;
    }
}

