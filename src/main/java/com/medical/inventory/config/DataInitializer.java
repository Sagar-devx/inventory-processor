package com.medical.inventory.config;

import com.medical.inventory.entity.AppUser;
import com.medical.inventory.entity.Role;
import com.medical.inventory.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    public CommandLineRunner initDefaultUsers(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            userRepository.findByUsername("admin").ifPresentOrElse(
                    admin -> {
                        admin.setPassword(passwordEncoder.encode("Admin@123"));
                        admin.setRole(Role.ROLE_ADMIN);
                        userRepository.save(admin);
                        log.info("Default admin user password refreshed with runtime BCrypt.");
                    },
                    () -> {
                        AppUser admin = new AppUser("admin", passwordEncoder.encode("Admin@123"), "admin@medical.com", Role.ROLE_ADMIN);
                        userRepository.save(admin);
                        log.info("Default admin user created successfully.");
                    }
            );

            userRepository.findByUsername("user").ifPresentOrElse(
                    user -> {
                        user.setPassword(passwordEncoder.encode("User@123"));
                        user.setRole(Role.ROLE_USER);
                        userRepository.save(user);
                        log.info("Default regular user password refreshed with runtime BCrypt.");
                    },
                    () -> {
                        AppUser user = new AppUser("user", passwordEncoder.encode("User@123"), "user@medical.com", Role.ROLE_USER);
                        userRepository.save(user);
                        log.info("Default regular user created successfully.");
                    }
            );
        };
    }
}
