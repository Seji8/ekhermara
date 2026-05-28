package com.example.pfe.services;

import com.example.pfe.dto.LoginRequest;
import com.example.pfe.models.User;
import com.example.pfe.repository.UserRepository;
import com.example.pfe.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Override
    public Map<String, Object> login(LoginRequest request) {

        // 1. Vérifier si utilisateur existe
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        // 2. Vérifier mot de passe
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Email ou mot de passe incorrect");
        }

        // 3. Générer token
        String token = jwtService.generateToken(user);

        // 4. Retourner réponse
        return Map.of(
                "token", token,
                "role", user.getRole().name(),
                "userId", user.getId(),
                "nom", user.getNom(),
                "email", user.getEmail(),
                "equipeId", user.getEquipe() != null ? user.getEquipe().getId() : 0L
        );
    }
}