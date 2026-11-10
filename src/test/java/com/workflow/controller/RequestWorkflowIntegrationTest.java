package com.workflow.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.dto.AuthRequest;
import com.workflow.dto.CreateRequestDto;
import com.workflow.dto.WorkflowDecisionDto;
import com.workflow.model.RequestCategory;
import com.workflow.model.RequestPriority;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class RequestWorkflowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String getJwtToken(String username, String password) throws Exception {
        AuthRequest loginRequest = new AuthRequest(username, password);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("data").path("token").asText();
    }

    @Test
    @DisplayName("Complete Workflow: Submit -> Approve -> View Audit Trail")
    void testCompleteApprovalWorkflow() throws Exception {
        String employeeToken = getJwtToken("employee1", "Employee@123");
        String managerToken = getJwtToken("manager1", "Manager@123");

        // 1. Employee submits a request
        CreateRequestDto createDto = CreateRequestDto.builder()
                .title("Ergonomic Chair Request")
                .description("Needed for lumbar support")
                .category(RequestCategory.EQUIPMENT)
                .priority(RequestPriority.MEDIUM)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/requests")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status", is("PENDING")))
                .andReturn();

        JsonNode createdNode = objectMapper.readTree(createResult.getResponse().getContentAsString());
        long requestId = createdNode.path("data").path("id").asLong();

        // 2. Manager approves the request
        WorkflowDecisionDto decision = new WorkflowDecisionDto("Approved for immediate procurement");
        mockMvc.perform(put("/api/v1/requests/" + requestId + "/approve")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(decision)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("APPROVED")))
                .andExpect(jsonPath("$.data.decisionComment", is("Approved for immediate procurement")));

        // 3. Verify audit history contains SUBMITTED and APPROVED
        mockMvc.perform(get("/api/v1/requests/" + requestId + "/history")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(2))));
    }

    @Test
    @DisplayName("RBAC Security: Non-admin users cannot access /api/v1/admin/stats")
    void testAdminEndpointRestricted() throws Exception {
        String employeeToken = getJwtToken("employee1", "Employee@123");

        mockMvc.perform(get("/api/v1/admin/stats")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("RBAC Security: Admin user can access /api/v1/admin/stats")
    void testAdminEndpointAllowedForAdmin() throws Exception {
        String adminToken = getJwtToken("admin", "Admin@123");

        mockMvc.perform(get("/api/v1/admin/stats")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalRequests", notNullValue()));
    }
}
