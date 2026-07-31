package com.khushi.requestapprovalworkflowsystem;

import com.khushi.requestapprovalworkflowsystem.entity.User;
import com.khushi.requestapprovalworkflowsystem.enums.Role;
import com.khushi.requestapprovalworkflowsystem.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class RequestApprovalWorkflowSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(RequestApprovalWorkflowSystemApplication.class, args);
    }

    @Bean
    public CommandLineRunner loadData(UserRepository userRepository) {
        return args -> {
            if (userRepository.count() == 0) {
                userRepository.save(User.builder().username("employee1").role(Role.EMPLOYEE).build());
                userRepository.save(User.builder().username("manager1").role(Role.MANAGER).build());
                System.out.println("Test users created: employee1 (EMPLOYEE), manager1 (MANAGER)");
            }
        };
    }
}
