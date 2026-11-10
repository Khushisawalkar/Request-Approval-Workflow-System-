package com.workflow.service;

import com.workflow.dto.AuthRequest;
import com.workflow.dto.AuthResponse;
import com.workflow.dto.RegisterRequest;
import com.workflow.exception.ResourceNotFoundException;
import com.workflow.model.RoleName;
import com.workflow.model.User;
import com.workflow.repository.UserRepository;
import com.workflow.security.JwtService;
import com.workflow.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already registered");
        }

        User manager = null;
        if (request.getManagerId() != null) {
            manager = userRepository.findById(request.getManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Manager not found with id: " + request.getManagerId()));
        }

        Set<RoleName> roles = (request.getRoles() != null && !request.getRoles().isEmpty())
                ? request.getRoles()
                : new HashSet<>(Set.of(RoleName.ROLE_EMPLOYEE));

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .department(request.getDepartment())
                .manager(manager)
                .roles(roles)
                .build();

        User savedUser = userRepository.save(user);
        log.info("New user registered successfully: username [{}]", savedUser.getUsername());

        UserPrincipal principal = new UserPrincipal(savedUser);
        String token = jwtService.generateToken(principal);

        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .username(savedUser.getUsername())
                .fullName(savedUser.getFullName())
                .roles(savedUser.getRoles().stream().map(Enum::name).collect(Collectors.toSet()))
                .build();
    }

    public AuthResponse login(AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.getUsername()));

        UserPrincipal principal = new UserPrincipal(user);
        String token = jwtService.generateToken(principal);
        log.info("User logged in successfully: username [{}]", user.getUsername());

        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .username(user.getUsername())
                .fullName(user.getFullName())
                .roles(user.getRoles().stream().map(Enum::name).collect(Collectors.toSet()))
                .build();
    }
}
