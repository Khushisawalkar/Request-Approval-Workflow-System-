package com.khushi.requestapprovalworkflowsystem.dto;

import lombok.Data;

@Data
public class CreateRequestDto {
    private Long employeeId;
    private String title;
    private String description;
}
