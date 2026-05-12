package com.example.pfe.controllers;

import com.example.pfe.dto.LoginRequest;
import com.example.pfe.dto.AuthResponse;
import com.example.pfe.models.User;
import com.example.pfe.repository.UserRepository;
import com.example.pfe.security.JwtService;

import com.example.pfe.services.EmailService;
import com.example.pfe.services.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserService userService;
    private final EmailService emailService;



    public AuthController(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          JwtService jwtService ,UserService userService, EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userService = userService;
        this.emailService = emailService;
    }
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");

        Optional<User> user = userService.getUserByEmail(email);

        if (user.isEmpty()) {
            // Return success even if email not found (security best practice)
            return ResponseEntity.ok(Map.of(
                    "message", "If your email exists in our system, you will receive reset instructions."
            ));
        }

        // Generate a temporary password
        String tempPassword = userService.generateTemporaryPassword();

        // Update user's password
        userService.updatePassword(email, tempPassword);

        // Send email with new password
        emailService.sendAccountEmail(email, email, tempPassword);

        return ResponseEntity.ok(Map.of(
                "message", "A new password has been sent to your email."
        ));
    }


    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Email ou mot de passe incorrect");
        }

        String token = jwtService.generateToken(user);

        return ResponseEntity.ok(Map.of(
                "token",    token,
                "role",     user.getRole().name(),
                "userId",   user.getId(),        // ← ajout
                "nom",      user.getNom(),       // ← ajout
                "email",    user.getEmail(),     // ← ajout
                "equipeId", user.getEquipe() != null ? user.getEquipe().getId() : 0L  // ← ajout
        ));
    }
}