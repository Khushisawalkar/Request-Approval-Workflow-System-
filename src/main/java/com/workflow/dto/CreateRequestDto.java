package com.workflow.dto;

import com.workflow.model.RequestCategory;
import com.workflow.model.RequestPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRequestDto {

    @NotBlank(message = "Title is required")
    @Size(max = 150, message = "Title cannot exceed 150 characters")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    @Builder.Default
    private RequestCategory category = RequestCategory.GENERAL;

    @Builder.Default
    private RequestPriority priority = RequestPriority.MEDIUM;

    private Long assignedManagerId;

    // Backward-compatibility field
    private Long employeeId;
}
