package com.khushi.requestapprovalworkflowsystem.entity;

import com.khushi.requestapprovalworkflowsystem.enums.Role;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String username;
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role;
}
