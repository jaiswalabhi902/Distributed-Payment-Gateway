package com.microservices.auth.config;

import com.microservices.auth.entity.Role;
import com.microservices.auth.entity.User;
import com.microservices.auth.repository.RoleRepository;
import com.microservices.auth.repository.UserRepository;
import com.microservices.common.constant.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bootstraps a default admin account on first startup. Roles and permissions are
 * seeded by Flyway; the admin password must be encoded at runtime, so it lives here.
 * Idempotent: does nothing if the admin user already exists.
 */
@Slf4j
@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.admin-username:admin}")
    private String adminUsername;
    @Value("${app.bootstrap.admin-email:admin@microservices.local}")
    private String adminEmail;
    @Value("${app.bootstrap.admin-password:admin123}")
    private String adminPassword;

    public DataInitializer(UserRepository userRepository,
                           RoleRepository roleRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.existsByUsername(adminUsername)) {
            return;
        }

        Role adminRole = roleRepository.findByName(Constants.Roles.ADMIN)
                .orElseThrow(() -> new IllegalStateException(
                        "Role " + Constants.Roles.ADMIN + " not seeded - check Flyway migrations"));

        User admin = User.builder()
                .username(adminUsername)
                .email(adminEmail)
                .password(passwordEncoder.encode(adminPassword))
                .build();
        admin.addRole(adminRole);
        userRepository.save(admin);

        log.warn("Bootstrapped default admin user '{}'. Change the password immediately.",
                adminUsername);
    }
}
