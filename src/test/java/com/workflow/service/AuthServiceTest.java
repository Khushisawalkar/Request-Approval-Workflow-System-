package com.workflow.service;

import com.workflow.dto.AuthRequest;
import com.workflow.dto.AuthResponse;
import com.workflow.dto.RegisterRequest;
import com.workflow.model.RoleName;
import com.workflow.model.User;
import com.workflow.repository.UserRepository;
import com.workflow.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private User testUser;

    @BeforeEach
    void setUp() {
        registerRequest = RegisterRequest.builder()
                .username("newuser")
                .email("newuser@workflow.com")
                .password("Password@123")
                .fullName("New Test User")
                .department("QA")
                .roles(Set.of(RoleName.ROLE_EMPLOYEE))
                .build();

        testUser = User.builder()
                .id(10L)
                .username("newuser")
                .email("newuser@workflow.com")
                .password("encoded_pass")
                .fullName("New Test User")
                .roles(Set.of(RoleName.ROLE_EMPLOYEE))
                .build();
    }

    @Test
    @DisplayName("Should successfully register a new user and generate a JWT")
    void testRegister_Success() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("newuser@workflow.com")).thenReturn(false);
        when(passwordEncoder.encode("Password@123")).thenReturn("encoded_pass");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtService.generateToken(any())).thenReturn("mock.jwt.token");

        AuthResponse response = authService.register(registerRequest);

        assertNotNull(response);
        assertEquals("newuser", response.getUsername());
        assertEquals("mock.jwt.token", response.getToken());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when registering duplicate username")
    void testRegister_DuplicateUsername() {
        when(userRepository.existsByUsername("newuser")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () ->
                authService.register(registerRequest)
        );

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should successfully authenticate and return JWT token")
    void testLogin_Success() {
        AuthRequest authRequest = new AuthRequest("newuser", "Password@123");

        when(userRepository.findByUsername("newuser")).thenReturn(Optional.of(testUser));
        when(jwtService.generateToken(any())).thenReturn("mock.jwt.token");

        AuthResponse response = authService.login(authRequest);

        assertNotNull(response);
        assertEquals("newuser", response.getUsername());
        assertEquals("mock.jwt.token", response.getToken());
        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }
}
