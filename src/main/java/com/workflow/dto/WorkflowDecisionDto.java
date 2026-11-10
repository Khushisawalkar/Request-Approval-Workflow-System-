package com.workflow.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowDecisionDto {

    @Size(max = 1000, message = "Comment cannot exceed 1000 characters")
    private String comment;
}
