package com.example.pfe;

import org.camunda.bpm.engine.RepositoryService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.example.pfe.models.Role;
import com.example.pfe.models.User;
import com.example.pfe.repository.UserRepository;

@SpringBootApplication
public class    PfeApplication {

    public static void main(String[] args) {
        // Disable Kerberos (GSS-API) authentication for JDBC connections
        System.setProperty("java.security.auth.login.config", "");
        System.setProperty("com.sun.security.sasl.gssapi.native", "false");
        SpringApplication.run(PfeApplication.class, args);
    }
    @Bean
    CommandLineRunner seedAdmin(UserRepository userRepository, PasswordEncoder encoder) {
          return args -> {
            final String email = "admin@example.com";
            if (userRepository.findByEmail(email).isEmpty()) {
                User admin = new User();
                admin.setNom("Admin");
                admin.setEmail(email);
                admin.setRole(Role.ADMIN);
                admin.setPassword(encoder.encode("admin123"));
                userRepository.save(admin);
                System.out.println("Seeded admin user: " + email + " / admin123");
            }
        };
    }
}
