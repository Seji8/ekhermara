package com.example.pfe.controllers;

import com.example.pfe.dto.LoginRequest;
import com.example.pfe.dto.CreateUserResponse;
import com.example.pfe.models.User;
import com.example.pfe.services.AuthService;
import com.example.pfe.services.EmailService;
import com.example.pfe.services.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final EmailService emailService;

    public AuthController(AuthService authService,
                          UserService userService,
                          EmailService emailService) {
        this.authService = authService;
        this.userService = userService;
        this.emailService = emailService;
    }

    // ✅ LOGIN PROPRE
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // ✅ FORGOT PASSWORD (inchangé)
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

    // ✅ GET CURRENT USER
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal User currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Non authentifié");
        }
        return ResponseEntity.ok(new CreateUserResponse(currentUser));
    }
}