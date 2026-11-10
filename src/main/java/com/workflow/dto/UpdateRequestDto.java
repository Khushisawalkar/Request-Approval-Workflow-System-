package com.workflow.dto;

import com.workflow.model.RequestCategory;
import com.workflow.model.RequestPriority;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRequestDto {

    @Size(max = 150, message = "Title cannot exceed 150 characters")
    private String title;

    private String description;

    private RequestCategory category;

    private RequestPriority priority;
}

