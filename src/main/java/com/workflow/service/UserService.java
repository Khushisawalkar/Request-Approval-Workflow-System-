package com.workflow.service;

import com.workflow.dto.UserSummaryDto;
import com.workflow.exception.ResourceNotFoundException;
import com.workflow.model.RoleName;
import com.workflow.model.User;
import com.workflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<UserSummaryDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToSummary)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserSummaryDto> getManagers() {
        return userRepository.findByRole(RoleName.ROLE_MANAGER).stream()
                .map(this::mapToSummary)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserSummaryDto getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        return mapToSummary(user);
    }

    public UserSummaryDto mapToSummary(User user) {
        if (user == null) {
            return null;
        }
        return UserSummaryDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .department(user.getDepartment())
                .roles(user.getRoles())
                .build();
    }
}
