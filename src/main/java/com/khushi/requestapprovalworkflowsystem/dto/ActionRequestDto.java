package com.khushi.requestapprovalworkflowsystem.dto;

import lombok.Data;

@Data
public class ActionRequestDto {
    private Long managerId;
    private String action; // "APPROVE" or "REJECT"
}
