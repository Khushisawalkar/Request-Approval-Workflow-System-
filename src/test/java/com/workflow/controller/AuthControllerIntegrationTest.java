package com.workflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.dto.AuthRequest;
import com.workflow.dto.RegisterRequest;
import com.workflow.model.RoleName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/v1/auth/register should create a user and return 201 with JWT")
    void testRegister_Success() throws Exception {
        RegisterRequest registerReq = RegisterRequest.builder()
                .username("integration_user")
                .email("integration@workflow.com")
                .password("Password@123")
                .fullName("Integration Tester")
                .department("Engineering")
                .roles(Set.of(RoleName.ROLE_EMPLOYEE))
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is(201)))
                .andExpect(jsonPath("$.data.username", is("integration_user")))
                .andExpect(jsonPath("$.data.token", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login with valid seed credentials should return 200 with JWT")
    void testLogin_Success() throws Exception {
        AuthRequest loginReq = new AuthRequest("employee1", "Employee@123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(200)))
                .andExpect(jsonPath("$.data.username", is("employee1")))
                .andExpect(jsonPath("$.data.token", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login with invalid password should return 401 Unauthorized")
    void testLogin_InvalidCredentials() throws Exception {
        AuthRequest loginReq = new AuthRequest("employee1", "WrongPassword!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isUnauthorized());
    }
}
