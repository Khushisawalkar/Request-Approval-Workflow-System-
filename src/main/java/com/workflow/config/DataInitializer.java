package com.workflow.config;

import com.workflow.model.*;
import com.workflow.repository.RequestAuditLogRepository;
import com.workflow.repository.RequestRepository;
import com.workflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RequestRepository requestRepository;
    private final RequestAuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            log.info("Bootstrapping initial system users, roles, and sample workflow requests...");

            // 1. Seed Administrator
            User admin = User.builder()
                    .username("admin")
                    .email("admin@workflow.com")
                    .password(passwordEncoder.encode("Admin@123"))
                    .fullName("System Administrator")
                    .department("IT Infrastructure")
                    .roles(Set.of(RoleName.ROLE_ADMIN, RoleName.ROLE_MANAGER, RoleName.ROLE_EMPLOYEE))
                    .build();
            userRepository.save(admin);

            // 2. Seed Manager
            User manager1 = User.builder()
                    .username("manager1")
                    .email("manager1@workflow.com")
                    .password(passwordEncoder.encode("Manager@123"))
                    .fullName("Alex Morgan (Engineering Manager)")
                    .department("Engineering")
                    .roles(Set.of(RoleName.ROLE_MANAGER, RoleName.ROLE_EMPLOYEE))
                    .build();
            userRepository.save(manager1);

            // 3. Seed Employees
            User employee1 = User.builder()
                    .username("employee1")
                    .email("employee1@workflow.com")
                    .password(passwordEncoder.encode("Employee@123"))
                    .fullName("Sarah Connor")
                    .department("Engineering")
                    .manager(manager1)
                    .roles(Set.of(RoleName.ROLE_EMPLOYEE))
                    .build();
            userRepository.save(employee1);

            User employee2 = User.builder()
                    .username("employee2")
                    .email("employee2@workflow.com")
                    .password(passwordEncoder.encode("Employee@123"))
                    .fullName("John Doe")
                    .department("Product Design")
                    .manager(manager1)
                    .roles(Set.of(RoleName.ROLE_EMPLOYEE))
                    .build();
            userRepository.save(employee2);

            // 4. Seed Requests & Audit Logs
            Request req1 = Request.builder()
                    .title("MacBook Pro M3 Max Workstation Upgrade")
                    .description("Required for local microservices and Docker container builds.")
                    .category(RequestCategory.EQUIPMENT)
                    .priority(RequestPriority.HIGH)
                    .status(RequestStatus.PENDING)
                    .submittedBy(employee1)
                    .assignedManager(manager1)
                    .build();
            requestRepository.save(req1);

            auditLogRepository.save(RequestAuditLog.builder()
                    .requestId(req1.getId())
                    .action("SUBMITTED")
                    .actorUsername("employee1")
                    .actorRole("ROLE_EMPLOYEE")
                    .fromStatus(null)
                    .toStatus(RequestStatus.PENDING)
                    .comment("Initial hardware upgrade request submitted")
                    .build());

            Request req2 = Request.builder()
                    .title("AWS Production Deployment Access")
                    .description("Access needed for deploying the upcoming Q4 workflow release.")
                    .category(RequestCategory.SOFTWARE_ACCESS)
                    .priority(RequestPriority.URGENT)
                    .status(RequestStatus.APPROVED)
                    .submittedBy(employee1)
                    .assignedManager(manager1)
                    .decisionComment("Approved after verifying security compliance training completion.")
                    .build();
            requestRepository.save(req2);

            auditLogRepository.save(RequestAuditLog.builder()
                    .requestId(req2.getId())
                    .action("APPROVED")
                    .actorUsername("manager1")
                    .actorRole("ROLE_MANAGER")
                    .fromStatus(RequestStatus.PENDING)
                    .toStatus(RequestStatus.APPROVED)
                    .comment("Approved after verifying security compliance training completion.")
                    .build());

            log.info("System bootstrap complete:");
            log.info("  Admin: admin / Admin@123");
            log.info("  Manager: manager1 / Manager@123");
            log.info("  Employee: employee1 / Employee@123");
        }
    }
}
