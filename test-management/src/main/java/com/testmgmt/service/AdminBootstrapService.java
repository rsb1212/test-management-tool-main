package com.testmgmt.service;

import com.testmgmt.entity.User;
import com.testmgmt.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrapService implements CommandLineRunner {

    private static final String ADMIN_EMAIL = "admin@testmgmt.com";
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "Admin@123";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        User admin = userRepository.findByEmail(ADMIN_EMAIL).orElseGet(() -> {
            User user = new User();
            user.setUsername(ADMIN_USERNAME);
            user.setEmail(ADMIN_EMAIL);
            return user;
        });

        admin.setRole(User.UserRole.ADMIN);
        admin.setActive(true);
        if (admin.getFullName() == null || admin.getFullName().isBlank()) {
            admin.setFullName("System Admin");
        }
        if (admin.getTeam() == null || admin.getTeam().isBlank()) {
            admin.setTeam("Platform");
        }

        if (admin.getPasswordHash() == null || !passwordEncoder.matches(ADMIN_PASSWORD, admin.getPasswordHash())) {
            admin.setPasswordHash(passwordEncoder.encode(ADMIN_PASSWORD));
            log.info("Reset seeded admin password for {}", ADMIN_EMAIL);
        }

        userRepository.save(admin);
    }
}
