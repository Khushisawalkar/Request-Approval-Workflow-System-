package com.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActionRequestDto {
    private Long managerId;
    private String action; // "APPROVE" or "REJECT"
    private String comment;
}

