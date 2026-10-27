
package com.stockflow.config;

import com.stockflow.model.AppUser;
import com.stockflow.repository.AppUserRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class InitialUserSeeder {

    @Bean
    public ApplicationRunner seedInitialUsers(
            AppUserRepository repository,
            PasswordEncoder encoder,
            @Value("${stockflow.security.admin-password}") String adminPassword,
            @Value("${stockflow.security.employee-password}") String employeePassword) {

        return args -> {
            if (repository.findByUsername("admin").isEmpty()) {
                repository.save(new AppUser(
                        "admin",
                        encoder.encode(adminPassword),
                        "ADMIN"
                ));
            }

            if (repository.findByUsername("employee").isEmpty()) {
                repository.save(new AppUser(
                        "employee",
                        encoder.encode(employeePassword),
                        "EMPLOYEE"
                ));
            }
        };
    }
}
