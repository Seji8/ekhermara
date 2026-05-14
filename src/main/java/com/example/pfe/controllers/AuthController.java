package com.example.pfe.controllers;

import com.example.pfe.dto.LoginRequest;
import com.example.pfe.dto.CreateUserResponse;
import com.example.pfe.models.User;
import com.example.pfe.repository.UserRepository;
import com.example.pfe.security.JwtService;
import com.example.pfe.services.EmailService;
import com.example.pfe.services.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
                          JwtService jwtService,
                          UserService userService,
                          EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userService = userService;
        this.emailService = emailService;
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
                "userId",   user.getId(),
                "nom",      user.getNom(),
                "email",    user.getEmail(),
                "equipeId", user.getEquipe() != null ? user.getEquipe().getId() : 0L
        ));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");

        Optional<User> user = userService.getUserByEmail(email);

        if (user.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "message", "If your email exists in our system, you will receive reset instructions."
            ));
        }

        String tempPassword = userService.generateTemporaryPassword();
        userService.updatePassword(email, tempPassword);
        emailService.sendAccountEmail(email, email, tempPassword);

        return ResponseEntity.ok(Map.of(
                "message", "A new password has been sent to your email."
        ));
    }

    // ── GET /auth/me — accessible to all authenticated roles ──
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal User currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Non authentifié");
        }
        return ResponseEntity.ok(new CreateUserResponse(currentUser));
    }
}